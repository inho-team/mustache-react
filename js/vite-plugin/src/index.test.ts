import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { DEFAULT_BASE, ENTRY_NAME, entrySource, mustacheReact } from './index.js';

type ConfigHook = (config: { root?: string }) => {
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
      config: { logger: { info: () => {} } },
    };
    (plugin.configureServer as unknown as (s: typeof server) => void)(server);

    listeners.change!(path.resolve('/app/src/main/resources/templates/dashboard.mustache'));
    listeners.change!(path.resolve('/app/src/main/resources/templates/notes.txt'));
    listeners.change!(path.resolve('/app/src/main/frontend/pages/A.mustache'));

    expect(watched).toEqual([path.resolve('/app/src/main/resources/templates')]);
    expect(sent).toEqual([{ type: 'full-reload', path: '*' }]);
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
});
