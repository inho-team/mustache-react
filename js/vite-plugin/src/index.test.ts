import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { DEFAULT_BASE, ENTRY_NAME, entrySource, mustacheReact } from './index.js';

type ConfigHook = (config: { root?: string }) => {
  base: string;
  build: { manifest: boolean; outDir: string; rollupOptions: { input: Record<string, string> } };
};

describe('mustacheReact', () => {
  it('writes a manifest into the Gradle static output by default', () => {
    const config = (mustacheReact().config as unknown as ConfigHook)({ root: '/app/src/main/frontend' });
    expect(config.base).toBe(DEFAULT_BASE);
    expect(config.build.manifest).toBe(true);
    expect(config.build.outDir).toBe(path.resolve('/app/build/mustache-react/static/mustache-react'));
    expect(Object.keys(config.build.rollupOptions.input)).toEqual([ENTRY_NAME]);
  });

  it('accepts another output directory', () => {
    const plugin = mustacheReact({ outDir: '../../../target/mustache-react/static/mustache-react' });
    const config = (plugin.config as unknown as ConfigHook)({ root: '/app/src/main/frontend' });
    expect(config.build.outDir).toBe(path.resolve('/app/target/mustache-react/static/mustache-react'));
  });

  it('generates an entry that loads pages, islands and the optional app wrapper', () => {
    const source = entrySource();
    expect(source).toContain("'/pages/**/*.{tsx,jsx}'");
    expect(source).toContain("'/islands/**/*.{tsx,jsx}'");
    expect(source).toContain("'/app.{tsx,jsx,ts,js}'");
  });
});
