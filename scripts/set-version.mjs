#!/usr/bin/env node
// Sets the version of every released artifact: the Java modules and build plugins (gradle.properties), the npm
// packages (js/*/package.json, and the VERSION constant of @mustache-react/client), and the examples that depend on
// them. Java and npm artifacts are always released with the same version.
//
//   node scripts/set-version.mjs 0.1.0
//   node scripts/set-version.mjs 0.2.0-SNAPSHOT
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const version = process.argv[2];
if (!version || !/^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$/.test(version)) {
  console.error('Usage: node scripts/set-version.mjs <major.minor.patch[-qualifier]>');
  process.exit(1);
}

const root = fileURLToPath(new URL('..', import.meta.url));

function edit(path, transform) {
  const file = root + path;
  const before = readFileSync(file, 'utf8');
  const after = transform(before);
  if (after === before && !before.includes(version)) {
    throw new Error(`${path}: nothing to update`);
  }
  writeFileSync(file, after);
  console.log(`  ${path}`);
}

console.log(`Setting version ${version}:`);
edit('gradle.properties', (s) => s.replace(/^version=.*$/m, `version=${version}`));
for (const pkg of ['client', 'server', 'vite-plugin']) {
  edit(`js/${pkg}/package.json`, (s) => s.replace(/"version": "[^"]*"/, `"version": "${version}"`));
}
edit('js/client/src/index.ts', (s) => s.replace(/export const VERSION = '[^']*';/, `export const VERSION = '${version}';`));
edit('examples/basic/pom.xml', (s) =>
  s.replace(/<mustache-react\.version>[^<]*<\/mustache-react\.version>/, `<mustache-react.version>${version}</mustache-react.version>`),
);
