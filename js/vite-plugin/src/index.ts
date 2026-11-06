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

/** Default port of the dev server. Must match `mustache-react.dev-server.url`. */
export const DEFAULT_DEV_PORT = 5173;

/** Mustache templates, relative to the frontend directory, for a Gradle/Maven project. */
export const DEFAULT_TEMPLATES_DIR = '../resources/templates';

export interface MustacheReactOptions {
  /**
   * Where to write the build. Spring must serve this directory at {@link base}.
   * Default: the `MUSTACHE_REACT_OUT_DIR` environment variable, else {@link DEFAULT_OUT_DIR}.
   */
  outDir?: string;
  /**
   * Public URL path of the assets. Must match `mustache-react.public-path`.
   * Default: `MUSTACHE_REACT_BASE`, else `/mustache-react/`.
   */
  base?: string;
  /** Dev server port. Must match `mustache-react.dev-server.url`. Default: `MUSTACHE_REACT_DEV_PORT`, else 5173. */
  port?: number;
  /**
   * Mustache templates to watch in development; the browser reloads when one changes.
   * Default: `MUSTACHE_REACT_TEMPLATES_DIR`, else `../resources/templates` (that is, `src/main/resources/templates`).
   * `false` turns it off.
   */
  templatesDir?: string | false;
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
    `bootstrap(import.meta.glob(['/pages/**/*.{tsx,jsx}', '/islands/**/*.{tsx,jsx}']), { wrapper, dev: import.meta.env.DEV });`,
    ``,
  ].join('\n');
}

/**
 * Vite plugin for mustache-react.
 *
 * Run Vite from the frontend directory (`src/main/frontend`). The plugin generates the client entry,
 * writes a manifest the Spring side reads, and puts the output where Spring serves static files.
 *
 * In development (`vite`), Spring pages load modules from this dev server
 * (`mustache-react.dev-server.enabled=true`): the plugin fixes the port, makes asset URLs absolute so they point here
 * rather than at Spring, and reloads the browser when a Mustache template changes.
 */
export function mustacheReact(options: MustacheReactOptions = {}): Plugin {
  const port = options.port ?? (Number(process.env.MUSTACHE_REACT_DEV_PORT) || DEFAULT_DEV_PORT);
  let templatesDir: string | undefined;
  return {
    name: 'mustache-react',
    config(userConfig) {
      const root = path.resolve(userConfig.root ?? process.cwd());
      const outDir = path.resolve(root, options.outDir ?? process.env.MUSTACHE_REACT_OUT_DIR ?? DEFAULT_OUT_DIR);
      const templates = options.templatesDir ?? process.env.MUSTACHE_REACT_TEMPLATES_DIR ?? DEFAULT_TEMPLATES_DIR;
      templatesDir = templates === false ? undefined : path.resolve(root, templates);
      return {
        base: options.base ?? process.env.MUSTACHE_REACT_BASE ?? DEFAULT_BASE,
        server: {
          port,
          strictPort: true,
          // The page comes from Spring (another origin): asset URLs must name the dev server.
          origin: `http://localhost:${port}`,
        },
        // Vite finds the dependencies to pre-bundle by scanning index.html, which Spring replaces. Scanning the
        // components instead avoids a full page reload when a page first imports a library.
        optimizeDeps: {
          entries: ['pages/**/*.{tsx,jsx}', 'islands/**/*.{tsx,jsx}', 'app.{tsx,jsx,ts,js}'],
          // Imported by @mustache-react/client from the generated entry, which the scan does not see.
          include: ['react', 'react-dom/client'],
        },
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
    configureServer(server) {
      if (!templatesDir) {
        return;
      }
      const dir = templatesDir;
      server.watcher.add(dir);
      const reload = (file: string) => {
        if (file.startsWith(dir) && file.endsWith('.mustache')) {
          server.config.logger.info(`mustache-react: ${path.relative(dir, file)} changed, reloading`, { timestamp: true });
          server.ws.send({ type: 'full-reload', path: '*' });
        }
      };
      server.watcher.on('change', reload);
      server.watcher.on('add', reload);
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
