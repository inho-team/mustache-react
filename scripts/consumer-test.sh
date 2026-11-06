#!/usr/bin/env bash
# Builds examples/consumer as an outside project would, against the artifacts of the current sources:
# the Java modules and Gradle plugin from the local Maven repository, the npm packages as packed tarballs.
# The Gradle plugin downloads Node.js and installs the npm dependencies from the registry; bootJar must produce one
# jar that serves the page rendered on the server.
#
#   scripts/consumer-test.sh [work-dir]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="${1:-$(mktemp -d)}"
PORT="${PORT:-18095}"
mkdir -p "$WORK"
WORK="$(cd "$WORK" && pwd)"
echo "Working in $WORK"

cd "$ROOT"
echo "== Publishing the Java artifacts to the local Maven repository"
./gradlew publishToMavenLocal -q
./gradlew -p build-plugins publishToMavenLocal -q

echo "== Packing the npm packages"
pnpm --filter "./js/*" run build >/dev/null
rm -rf "$WORK/packages" && mkdir -p "$WORK/packages"
for pkg in client server vite-plugin; do
  (cd "js/$pkg" && pnpm pack --pack-destination "$WORK/packages" >/dev/null)
done
ls "$WORK/packages"

echo "== Copying examples/consumer"
rm -rf "$WORK/app"
mkdir -p "$WORK/app"
tar -C examples/consumer --exclude=build --exclude=.gradle --exclude=node_modules --exclude=.mustache-react -cf - . \
  | tar -C "$WORK/app" -xf -
# Point the @mustache-react/* dependencies at the tarballs; overrides do the same for the packages' own
# dependencies on each other (@mustache-react/server -> @mustache-react/client).
node - "$WORK" <<'NODE'
const fs = require('fs');
const path = require('path');
const work = process.argv[2];
const file = path.join(work, 'app/src/main/frontend/package.json');
const pkg = JSON.parse(fs.readFileSync(file, 'utf8'));
const tarballs = {};
for (const name of fs.readdirSync(path.join(work, 'packages'))) {
  const id = '@mustache-react/' + name.replace(/^mustache-react-/, '').replace(/-\d.*$/, '');
  tarballs[id] = 'file:' + path.join(work, 'packages', name);
}
for (const section of ['dependencies', 'devDependencies']) {
  for (const id of Object.keys(pkg[section] ?? {})) {
    if (tarballs[id]) pkg[section][id] = tarballs[id];
  }
}
pkg.overrides = tarballs;
fs.writeFileSync(file, JSON.stringify(pkg, null, 2) + '\n');
NODE

echo "== bootJar (downloads Node.js, npm install, frontend and SSR build, type generation)"
cd "$WORK/app"
"$ROOT/gradlew" -p "$WORK/app" bootJar --console=plain
test -x .mustache-react/node/*/bin/node || { echo "Node.js was not downloaded" >&2; exit 1; }
JAR=$(ls build/libs/*.jar | grep -v plain | head -1)
# Spring Boot moves META-INF/ of the resources to the jar root, which is on the class path too.
# (The listing goes through a variable: grep -q closing the pipe early would fail the pipeline under pipefail.)
LISTING=$(unzip -l "$JAR")
for entry in BOOT-INF/classes/static/mustache-react/.vite/manifest.json META-INF/mustache-react/ssr/server.mjs \
  BOOT-INF/lib/mustache-react-spring-boot-starter-; do
  grep -qF "$entry" <<<"$LISTING" || { echo "$JAR lacks $entry" >&2; exit 1; }
done

echo "== Type check against the generated types"
(cd src/main/frontend && node node_modules/typescript/bin/tsc --noEmit)

echo "== Running $JAR"
# The application finds node on the PATH or in .mustache-react/node (the working directory).
java -jar "$JAR" --server.port="$PORT" > "$WORK/app.log" 2>&1 &
APP=$!
trap 'kill $APP 2>/dev/null || true' EXIT
for _ in $(seq 1 60); do
  curl -sf -o /dev/null "http://localhost:$PORT/" && break
  sleep 1
done

HTML=$(curl -sf -H 'Accept: text/html' "http://localhost:$PORT/")
JSON=$(curl -sf -H 'Accept: application/json' "http://localhost:$PORT/")
fail() { echo "FAILED: $1" >&2; echo "$HTML" >&2; cat "$WORK/app.log" >&2; exit 1; }
grep -q 'data-mr-island="pages/Hello" data-mr-id="mr-page" data-mr-ssr="true"><main><h1>Hello from Spring</h1>' <<<"$HTML" \
  || fail "the page was not rendered on the server"
SCRIPT=$(grep -o 'src="/mustache-react/assets/[^"]*\.js"' <<<"$HTML" | head -1 | sed 's/src="//; s/"$//')
curl -sf -o /dev/null "http://localhost:$PORT$SCRIPT" || fail "the entry script $SCRIPT is not served"
[[ "$JSON" == '{"message":"Hello from Spring","frameworks":["Spring Boot","React","Vite"]}' ]] || fail "unexpected JSON: $JSON"

echo "Consumer test passed: Node.js downloaded, npm packages installed, one jar, server-side rendered page."
