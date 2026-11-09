import { createRequire } from 'node:module';
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

/** Entry of the server-side rendering bundle, built with `build.ssr` and loaded by the dev server. */
export const SSR_ENTRY_ID = 'virtual:mustache-react/ssr-entry';
const RESOLVED_SSR_ENTRY_ID = '\0' + SSR_ENTRY_ID;

/** File name of the SSR bundle. Spring looks for `META-INF/mustache-react/ssr/server.mjs` on the class path. */
export const SSR_BUNDLE = 'server.mjs';

/** Output directory of the SSR bundle, relative to the frontend directory, for a Gradle project. */
export const DEFAULT_SSR_OUT_DIR = '../../../build/mustache-react/ssr';

/** Written next to `.vite/` in the client build; Spring compares the version with its own. */
export const BUILD_INFO = 'mustache-react-build.json';

/** Version of this package, as in its package.json. */
export const PLUGIN_VERSION: string = (createRequire(import.meta.url)('../package.json') as { version: string }).version;

/** Render endpoint on the SSR server and the dev server. Same path as in `@mustache-react/server`. */
export const RENDER_PATH = '/__mustache-react/render';

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
   * Public URL path of the assets on the dev server. Must match `mustache-react.public-path` in development.
   * Default: `MUSTACHE_REACT_BASE`, else `/mustache-react/`. Builds always use relative URLs (`./`), so the built
   * files work wherever they are served: from the application jar or from a CDN (`mustache-react.public-path`
   * set to its URL), without rebuilding.
   */
  base?: string;
  /** Dev server port. Must match `mustache-react.dev-server.url`. Default: `MUSTACHE_REACT_DEV_PORT`, else 5173. */
  port?: number;
  /**
   * Where `build --ssr` writes the SSR bundle. Default: `MUSTACHE_REACT_SSR_OUT_DIR`, else {@link DEFAULT_SSR_OUT_DIR}.
   */
  ssrOutDir?: string;
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
 * Source of the generated SSR entry: the same components and wrapper as the client entry, imported eagerly, and an
 * HTTP server started when the bundle is run directly (`node server.mjs --port 5174`).
 */
export function ssrEntrySource(): string {
  return [
    `import { pathToFileURL } from 'node:url';`,
    `import { createRenderer, portFromArgs, startServer } from '@mustache-react/server';`,
    `const app = import.meta.glob('/app.{tsx,jsx,ts,js}', { eager: true });`,
    `export const render = createRenderer({`,
    `  components: import.meta.glob(['/pages/**/*.{tsx,jsx}', '/islands/**/*.{tsx,jsx}'], { eager: true }),`,
    `  wrapper: Object.values(app)[0]?.default,`,
    `});`,
    `if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {`,
    `  const port = portFromArgs(process.argv);`,
    `  startServer(render, port).then(() => console.log('mustache-react SSR server listening on 127.0.0.1:' + port));`,
    `}`,
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
  let ssrBuild = false;
  return {
    name: 'mustache-react',
    generateBundle() {
      if (!ssrBuild) {
        this.emitFile({ type: 'asset', fileName: BUILD_INFO, source: JSON.stringify({ version: PLUGIN_VERSION }) + '\n' });
      }
    },
    config(userConfig, env) {
      const root = path.resolve(userConfig.root ?? process.cwd());
      ssrBuild = Boolean(env?.isSsrBuild || userConfig.build?.ssr);
      if (ssrBuild) {
        // Vite 8 bundles with Rolldown, which renamed Rollup's inlineDynamicImports.
        const rolldown = 'rolldownVersion' in ((this as { meta?: object } | undefined)?.meta ?? {});
        const singleFile = rolldown ? { codeSplitting: false } : { inlineDynamicImports: true };
        // One self-contained file: Spring runs it with plain `node`, without node_modules.
        const ssrOutDir = path.resolve(root, options.ssrOutDir ?? process.env.MUSTACHE_REACT_SSR_OUT_DIR ?? DEFAULT_SSR_OUT_DIR);
        return {
          base: options.base ?? process.env.MUSTACHE_REACT_BASE ?? DEFAULT_BASE,
          ssr: { noExternal: true, target: 'node' },
          build: {
            ssr: true,
            outDir: ssrOutDir,
            emptyOutDir: true,
            manifest: false,
            rollupOptions: {
              input: SSR_ENTRY_ID,
              output: { entryFileNames: SSR_BUNDLE, ...singleFile },
            },
          },
        };
      }
      const outDir = path.resolve(root, options.outDir ?? process.env.MUSTACHE_REACT_OUT_DIR ?? DEFAULT_OUT_DIR);
      const templates = options.templatesDir ?? process.env.MUSTACHE_REACT_TEMPLATES_DIR ?? DEFAULT_TEMPLATES_DIR;
      templatesDir = templates === false ? undefined : path.resolve(root, templates);
      return {
        // Built chunks load each other and their CSS relative to their own URL: the same files work in the jar and
        // on a CDN. The dev server needs the absolute path Spring points to.
        base: env?.command === 'build' ? './' : (options.base ?? process.env.MUSTACHE_REACT_BASE ?? DEFAULT_BASE),
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
      // Server-side rendering in development: Spring posts here; the renderer is loaded anew for each request.
      server.middlewares.use(RENDER_PATH, (req, res, next) => {
        if (req.method !== 'POST') {
          next();
          return;
        }
        const chunks: Buffer[] = [];
        req.on('data', (chunk: Buffer) => chunks.push(chunk));
        req.on('end', () => {
          void (async () => {
            let status = 200;
            let json: unknown;
            try {
              const request = JSON.parse(Buffer.concat(chunks).toString('utf8')) as Record<string, unknown>;
              const module = (await server.ssrLoadModule(SSR_ENTRY_ID)) as { render: (r: unknown) => string };
              json = { html: module.render(request) };
            } catch (error) {
              if (error instanceof Error) {
                server.ssrFixStacktrace(error);
              }
              status = 500;
              json = { error: error instanceof Error ? (error.stack ?? error.message) : String(error) };
            }
            res.statusCode = status;
            res.setHeader('Content-Type', 'application/json; charset=utf-8');
            res.end(JSON.stringify(json));
          })();
        });
      });
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
      if (id === ENTRY_ID) {
        return RESOLVED_ENTRY_ID;
      }
      return id === SSR_ENTRY_ID ? RESOLVED_SSR_ENTRY_ID : undefined;
    },
    load(id) {
      if (id === RESOLVED_ENTRY_ID) {
        return entrySource();
      }
      return id === RESOLVED_SSR_ENTRY_ID ? ssrEntrySource() : undefined;
    },
  };
}

export default mustacheReact;
