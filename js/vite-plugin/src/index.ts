import type { Plugin } from 'vite';

export interface MustacheReactOptions {
  /** Directory holding the React sources. Default: `src/main/frontend`. */
  frontendDir?: string;
}

/**
 * Vite plugin for mustache-react.
 *
 * Status: skeleton in M0. Manifest output, page discovery and Mustache reload are implemented in M1–M3.
 */
export function mustacheReact(options: MustacheReactOptions = {}): Plugin {
  const frontendDir = options.frontendDir ?? 'src/main/frontend';
  return {
    name: 'mustache-react',
    config() {
      return {
        root: frontendDir,
        build: { manifest: true },
      };
    },
  };
}

export default mustacheReact;
