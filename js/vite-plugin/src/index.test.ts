import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { publicDirWarning, BUILD_INFO, DEFAULT_BASE, ENTRY_NAME, entrySource, mustacheReact, PLUGIN_VERSION, ssrEntrySource } from './index.js';

type ConfigHook = (config: { root?: string }, env?: { command: 'build' | 'serve'; mode: string }) => {
  base: string;
  server: { port: number; strictPort: boolean; origin: string };
  optimizeDeps: { entries: string[] };
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

  it('builds with relative asset URLs, so the files work in the jar and on a CDN', () => {
    const config = (mustacheReact({ base: '/assets/' }).config as unknown as ConfigHook)(
      { root: '/app/src/main/frontend' },
      { command: 'build', mode: 'production' },
    );
    expect(config.base).toBe('./');
    const dev = (mustacheReact({ base: '/assets/' }).config as unknown as ConfigHook)(
      { root: '/app/src/main/frontend' },
      { command: 'serve', mode: 'development' },
    );
    expect(dev.base).toBe('/assets/');
  });

  it('serves on a fixed port with absolute asset URLs for pages served by Spring', () => {
    const config = (mustacheReact({ port: 5199 }).config as unknown as ConfigHook)({ root: '/app/src/main/frontend' });
    expect(config.server).toEqual({ port: 5199, strictPort: true, origin: 'http://localhost:5199' });
    expect(config.optimizeDeps.entries).toContain('islands/**/*.{tsx,jsx}');
  });

  it('reloads the browser when a Mustache template changes', () => {
    const plugin = mustacheReact();
    (plugin.config as unknown as ConfigHook)({ root: '/app/src/main/frontend' });
    const listeners: Record<string, (file: string) => void> = {};
    const sent: unknown[] = [];
    const watched: string[] = [];
    const server = {
      watcher: { add: (dir: string) => watched.push(dir), on: (event: string, fn: (file: string) => void) => (listeners[event] = fn) },
      ws: { send: (payload: unknown) => sent.push(payload) },
      middlewares: { use: () => {} },
      config: { logger: { info: () => {} } },
    };
    (plugin.configureServer as unknown as (s: typeof server) => void)(server);

    listeners.change!(path.resolve('/app/src/main/resources/templates/dashboard.mustache'));
    listeners.change!(path.resolve('/app/src/main/resources/templates/notes.txt'));
    listeners.change!(path.resolve('/app/src/main/frontend/pages/A.mustache'));

    expect(watched).toEqual([path.resolve('/app/src/main/resources/templates')]);
    expect(sent).toEqual([{ type: 'full-reload', path: '*' }]);
  });

  it('builds a single self-contained SSR bundle', () => {
    const config = (mustacheReact().config as unknown as (c: unknown, e: unknown) => Record<string, any>)(
      { root: '/app/src/main/frontend' },
      { isSsrBuild: true },
    );
    expect(config.ssr).toEqual({ noExternal: true, target: 'node' });
    expect(config.build.outDir).toBe(path.resolve('/app/build/mustache-react/ssr'));
    expect(config.build.ssr).toBe(true);
    expect(config.build.rollupOptions.input).toBe('virtual:mustache-react/ssr-entry');
    expect(config.build.rollupOptions.output).toEqual({ entryFileNames: 'server.mjs', inlineDynamicImports: true });
    const rolldown = (mustacheReact().config as unknown as (this: unknown, c: unknown, e: unknown) => Record<string, any>).call(
      { meta: { rolldownVersion: '1.0.0' } },
      { root: '/app/src/main/frontend' },
      { isSsrBuild: true },
    );
    expect(rolldown.build.rollupOptions.output).toEqual({ entryFileNames: 'server.mjs', codeSplitting: false });
  });

  it('generates an SSR entry that serves only when run directly', () => {
    const source = ssrEntrySource();
    expect(source).toContain("from '@mustache-react/server'");
    expect(source).toContain('{ eager: true }');
    expect(source).toContain('import.meta.url === pathToFileURL(process.argv[1]).href');
  });

  it('writes its version next to the manifest in client builds only', () => {
    const emitted: unknown[] = [];
    const context = { emitFile: (file: unknown) => emitted.push(file) };
    const plugin = mustacheReact();
    (plugin.config as unknown as (c: unknown, e: unknown) => unknown)({ root: '/app/src/main/frontend' }, { isSsrBuild: false });
    (plugin.generateBundle as unknown as (this: typeof context) => void).call(context);
    const ssr = mustacheReact();
    (ssr.config as unknown as (c: unknown, e: unknown) => unknown)({ root: '/app/src/main/frontend' }, { isSsrBuild: true });
    (ssr.generateBundle as unknown as (this: typeof context) => void).call(context);

    expect(emitted).toEqual([{ type: 'asset', fileName: BUILD_INFO, source: `{"version":"${PLUGIN_VERSION}"}\n` }]);
    expect(PLUGIN_VERSION).toMatch(/^\d+\.\d+\.\d+/);
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
    expect(source).toContain('dev: import.meta.env.DEV');
  });

  it('warns when public/ has files, which are not served at the site root', () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'mr-public-'));
    fs.mkdirSync(path.join(dir, 'img'));
    fs.writeFileSync(path.join(dir, 'img', 'logo.png'), '');
    const warnings: string[] = [];
    const run = (command: 'build' | 'serve', publicDir: string) => {
      const plugin = mustacheReact();
      (plugin.config as unknown as ConfigHook)({ root: '/app/src/main/frontend' }, { command, mode: 'production' });
      (plugin.configResolved as unknown as (c: object) => void)({ command, publicDir });
      (plugin.buildStart as unknown as (this: { warn: (m: string) => void }) => void).call({ warn: (m) => warnings.push(m) });
    };
    run('build', dir);
    expect(warnings).toEqual([publicDirWarning(['img/logo.png'])]);
    expect(warnings[0]).toContain('/mustache-react/logo.png');
    run('serve', dir);
    run('build', path.join(dir, 'missing'));
    expect(warnings).toHaveLength(1);
  });
});
