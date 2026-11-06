#!/usr/bin/env bash
# Uploads the staged artifacts (build/staging-deploy) to Maven Central through the Central Portal API.
#
#   ./gradlew publishAllPublicationsToStagingRepository
#   ./gradlew -p build-plugins publishAllPublicationsToStagingRepository
#   CENTRAL_USERNAME=... CENTRAL_PASSWORD=... scripts/publish-central.sh
#
# CENTRAL_USERNAME / CENTRAL_PASSWORD are a user token generated on https://central.sonatype.com/account.
# --dry-run builds and checks the bundle without uploading it.
# PUBLISHING_TYPE=USER_MANAGED leaves the deployment for a manual "Publish" in the portal (default: AUTOMATIC).
set -euo pipefail

cd "$(dirname "$0")/.."
STAGING=build/staging-deploy
BUNDLE=build/central-bundle.zip
DRY_RUN=false
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=true

VERSION=$(sed -n 's/^version=//p' gradle.properties)
if [[ "$VERSION" == *-SNAPSHOT ]]; then
  echo "Refusing to publish $VERSION: Maven Central does not take snapshots. Run scripts/set-version.mjs first." >&2
  exit 1
fi
if [[ ! -d "$STAGING/run/iknow" ]]; then
  echo "Nothing staged in $STAGING. Run publishAllPublicationsToStagingRepository for both builds." >&2
  exit 1
fi

# The portal rejects repository metadata; every artifact needs its signature and checksums.
find "$STAGING" -name 'maven-metadata.xml*' -delete
missing=0
while IFS= read -r file; do
  for suffix in .asc .md5 .sha1; do
    if [[ ! -f "$file$suffix" ]]; then
      echo "Missing $file$suffix" >&2
      missing=1
    fi
  done
done < <(find "$STAGING" -type f \( -name '*.jar' -o -name '*.pom' -o -name '*.module' \))
if [[ $missing == 1 ]]; then
  echo "Sign the artifacts: set SIGNING_KEY and SIGNING_PASSWORD when running the Gradle publish tasks." >&2
  exit 1
fi

rm -f "$BUNDLE"
(cd "$STAGING" && zip -qr "../$(basename "$BUNDLE")" .)
echo "Bundle $BUNDLE: $(find "$STAGING" -name '*.pom' | wc -l) components, version $VERSION"
if [[ $DRY_RUN == true ]]; then
  echo "Dry run: not uploading."
  exit 0
fi

: "${CENTRAL_USERNAME:?Set CENTRAL_USERNAME (Central Portal user token)}"
: "${CENTRAL_PASSWORD:?Set CENTRAL_PASSWORD (Central Portal user token)}"
AUTH="Authorization: Bearer $(printf '%s:%s' "$CENTRAL_USERNAME" "$CENTRAL_PASSWORD" | base64 | tr -d '\n')"
API=https://central.sonatype.com/api/v1/publisher

DEPLOYMENT=$(curl --fail-with-body -sS -X POST -H "$AUTH" \
  -F "bundle=@$BUNDLE" "$API/upload?name=mustache-react-$VERSION&publishingType=${PUBLISHING_TYPE:-AUTOMATIC}")
echo "Uploaded deployment $DEPLOYMENT"

# Validation and publishing take a few minutes.
for _ in $(seq 1 90); do
  STATUS=$(curl --fail-with-body -sS -X POST -H "$AUTH" "$API/status?id=$DEPLOYMENT")
  STATE=$(printf '%s' "$STATUS" | sed -n 's/.*"deploymentState" *: *"\([A-Z_]*\)".*/\1/p')
  echo "  $STATE"
  case "$STATE" in
    PUBLISHED) echo "Published mustache-react $VERSION to Maven Central."; exit 0 ;;
    VALIDATED) if [[ "${PUBLISHING_TYPE:-AUTOMATIC}" == USER_MANAGED ]]; then echo "Validated; publish it in the portal."; exit 0; fi ;;
    FAILED) echo "$STATUS" >&2; exit 1 ;;
  esac
  sleep 20
done
echo "Timed out waiting for deployment $DEPLOYMENT; check https://central.sonatype.com/publishing/deployments" >&2
exit 1
