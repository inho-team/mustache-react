import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { findConfigFile, parseArgs } from './cli.js';

describe('parseArgs', () => {
  it('copies options to the environment variables read by the plugin', () => {
    const env: NodeJS.ProcessEnv = {};
    expect(parseArgs(['build', '--out-dir', '/out', '--port=5199', '--templates-dir', 't', '--base', '/b/'], env))
      .toEqual({ command: 'build' });
    expect(env).toEqual({
      MUSTACHE_REACT_OUT_DIR: '/out',
      MUSTACHE_REACT_DEV_PORT: '5199',
      MUSTACHE_REACT_TEMPLATES_DIR: 't',
      MUSTACHE_REACT_BASE: '/b/',
    });
  });

  it('rejects unknown commands and options', () => {
    expect(() => parseArgs(['serve'], {})).toThrow('Usage: mustache-react-vite build|dev');
    expect(() => parseArgs(['dev', '--open'], {})).toThrow('Unknown option --open');
    expect(() => parseArgs(['dev', '--port'], {})).toThrow('Missing value for --port');
  });
});

describe('findConfigFile', () => {
  it('finds a project config or none', () => {
    const dir = mkdtempSync(path.join(tmpdir(), 'mr-cli-'));
    expect(findConfigFile(dir)).toBeUndefined();
    writeFileSync(path.join(dir, 'vite.config.mjs'), 'export default {}');
    expect(findConfigFile(dir)).toBe(path.join(dir, 'vite.config.mjs'));
  });
});
