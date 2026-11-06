import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { VERSION } from './index.js';

const read = (path: string) => readFileSync(fileURLToPath(new URL(`../../../${path}`, import.meta.url)), 'utf8');

describe('version', () => {
  it('is the same for the Java artifacts and every npm package (scripts/set-version.mjs)', () => {
    const java = /^version=(.*)$/m.exec(read('gradle.properties'))![1];
    expect(VERSION).toBe(java);
    for (const pkg of ['client', 'server', 'vite-plugin']) {
      expect(JSON.parse(read(`js/${pkg}/package.json`)).version, pkg).toBe(java);
    }
  });
});
