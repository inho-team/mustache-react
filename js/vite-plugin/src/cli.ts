/**
 * `mustache-react-vite build|dev`: runs Vite for a mustache-react frontend directory.
 *
 * The Gradle and Maven plugins, and Spring when it starts the dev server, call this instead of `vite`, so that a
 * project needs no `vite.config.ts`: without one, the defaults are React (`@vitejs/plugin-react`) plus this plugin.
 * With one, it is used as is (it should list `mustacheReact()`).
 *
 * Options, also readable from the environment (handy for build tools):
 *   --out-dir <dir>         MUSTACHE_REACT_OUT_DIR        where `build` writes the files Spring serves
 *   --base <path>           MUSTACHE_REACT_BASE           public URL path, default /mustache-react/
 *   --port <n>              MUSTACHE_REACT_DEV_PORT       dev server port, default 5173
 *   --templates-dir <dir>   MUSTACHE_REACT_TEMPLATES_DIR  Mustache templates watched by `dev`
 * Runs in the current directory (the frontend directory).
 */
import { existsSync } from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import type { InlineConfig, PluginOption } from 'vite';
import { mustacheReact } from './index.js';

const CONFIG_FILES = ['vite.config.ts', 'vite.config.mts', 'vite.config.cts', 'vite.config.js', 'vite.config.mjs', 'vite.config.cjs'];

const OPTIONS: Record<string, string> = {
  '--out-dir': 'MUSTACHE_REACT_OUT_DIR',
  '--base': 'MUSTACHE_REACT_BASE',
  '--port': 'MUSTACHE_REACT_DEV_PORT',
  '--templates-dir': 'MUSTACHE_REACT_TEMPLATES_DIR',
};

export interface CliArgs {
  command: 'build' | 'dev';
}

/** Parses the arguments; options are copied to the environment variables the plugin reads. */
export function parseArgs(argv: string[], env: NodeJS.ProcessEnv = process.env): CliArgs {
  const [command, ...rest] = argv;
  if (command !== 'build' && command !== 'dev') {
    throw new Error(`Usage: mustache-react-vite build|dev [${Object.keys(OPTIONS).join(' <value>] [')} <value>]`);
  }
  for (let i = 0; i < rest.length; i++) {
    const arg = rest[i]!;
    const [name, inline] = arg.includes('=') ? [arg.slice(0, arg.indexOf('=')), arg.slice(arg.indexOf('=') + 1)] : [arg, undefined];
    const variable = OPTIONS[name];
    if (!variable) {
      throw new Error(`Unknown option ${name}`);
    }
    const value = inline ?? rest[++i];
    if (value === undefined) {
      throw new Error(`Missing value for ${name}`);
    }
    env[variable] = value;
  }
  return { command };
}

/** The project's own Vite config file, if any. */
export function findConfigFile(root: string): string | undefined {
  return CONFIG_FILES.map((name) => path.join(root, name)).find((file) => existsSync(file));
}

/** Imports a package the way the frontend project sees it (from its node_modules). */
async function importFromProject(root: string, name: string): Promise<Record<string, unknown>> {
  const require = createRequire(path.join(root, 'package.json'));
  let resolved: string;
  try {
    resolved = require.resolve(name);
  } catch {
    throw new Error(`${name} is not installed in ${root}. Add it to devDependencies.`);
  }
  return (await import(pathToFileURL(resolved).href)) as Record<string, unknown>;
}

async function viteConfig(root: string): Promise<InlineConfig> {
  const configFile = findConfigFile(root);
  if (configFile) {
    return { root, configFile };
  }
  const react = (await importFromProject(root, '@vitejs/plugin-react')).default as () => PluginOption;
  return { root, configFile: false, plugins: [react(), mustacheReact()] };
}

export async function run(argv: string[]): Promise<void> {
  const { command } = parseArgs(argv);
  const root = process.cwd();
  const vite = await import('vite');
  const config = await viteConfig(root);

  if (command === 'build') {
    await vite.build(config);
    return;
  }
  const server = await vite.createServer(config);
  await server.listen();
  server.printUrls();
  const stop = () => {
    void server.close().finally(() => process.exit(0));
  };
  process.on('SIGTERM', stop);
  process.on('SIGINT', stop);
}
