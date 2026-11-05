import path from 'node:path';
import type { Plugin } from 'vite';

/** Name of the client entry chunk. The Java side looks it up in the Vite manifest by this name. */
export const ENTRY_NAME = 'mustache-react';

/** Public URL path under which Spring serves the built assets (`classpath:/static/mustache-react/`). */
export const DEFAULT_BASE = '/mustache-react/';

/** Output directory, relative to the frontend directory, for a Gradle project (`src/main/frontend`). */
export const DEFAULT_OUT_DIR = '../../../build/mustache-react/static/mustache-react';

const ENTRY_ID = 'virtual:mustache-react/entry';
const RESOLVED_ENTRY_ID = '\0' + ENTRY_ID;

export interface MustacheReactOptions {
  /**
   * Where to write the build. Spring must serve this directory at {@link base}.
   * Default: the `MUSTACHE_REACT_OUT_DIR` environment variable, else {@link DEFAULT_OUT_DIR}.
   */
  outDir?: string;
  /** Public URL path of the assets. Must match `mustache-react.public-path`. Default: `/mustache-react/`. */
  base?: string;
}

/**
 * Source of the generated client entry.
 *
 * Every file under `pages/` and `islands/` becomes a lazily loaded component, and `app.tsx` (optional)
 * provides a wrapper around every island.
 */
export function entrySource(): string {
  return [
    `import { bootstrap } from '@mustache-react/client';`,
    `const app = import.meta.glob('/app.{tsx,jsx,ts,js}', { eager: true });`,
    `const wrapper = Object.values(app)[0]?.default;`,
    `bootstrap(import.meta.glob(['/pages/**/*.{tsx,jsx}', '/islands/**/*.{tsx,jsx}']), { wrapper });`,
    ``,
  ].join('\n');
}

/**
 * Vite plugin for mustache-react.
 *
 * Run Vite from the frontend directory (`src/main/frontend`). The plugin generates the client entry,
 * writes a manifest the Spring side reads, and puts the output where Spring serves static files.
 */
export function mustacheReact(options: MustacheReactOptions = {}): Plugin {
  return {
    name: 'mustache-react',
    config(userConfig) {
      const root = path.resolve(userConfig.root ?? process.cwd());
      const outDir = path.resolve(root, options.outDir ?? process.env.MUSTACHE_REACT_OUT_DIR ?? DEFAULT_OUT_DIR);
      return {
        base: options.base ?? DEFAULT_BASE,
        build: {
          manifest: true,
          outDir,
          emptyOutDir: true,
          rollupOptions: {
            input: { [ENTRY_NAME]: ENTRY_ID },
          },
        },
      };
    },
    resolveId(id) {
      return id === ENTRY_ID ? RESOLVED_ENTRY_ID : undefined;
    },
    load(id) {
      return id === RESOLVED_ENTRY_ID ? entrySource() : undefined;
    },
  };
}

export default mustacheReact;
