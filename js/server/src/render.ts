import { createElement, Fragment, type ComponentType, type ReactNode } from 'react';
import { renderToString } from 'react-dom/server';
import { ServerContextProvider, toIslandName, type ServerContext } from '@mustache-react/client';

/** A render request from Spring. */
export interface RenderRequest {
  /** Component path, e.g. `pages/orders/OrderList`, as in `data-mr-island`. */
  component: string;
  props: Record<string, unknown>;
  /** The server context of the page (CSRF, user, locale, ...). */
  context?: ServerContext;
}

export interface RendererOptions {
  /** Eagerly imported component modules, keyed by path, as returned by `import.meta.glob(..., { eager: true })`. */
  components: Record<string, unknown>;
  /** The `app.tsx` wrapper, if any. Must match the one the browser uses, or hydration fails. */
  wrapper?: ComponentType<{ children?: ReactNode }>;
}

export type Renderer = (request: RenderRequest) => string;

/**
 * Renders components to HTML for hydration. The tree matches what `bootstrap()` renders in the browser
 * (wrapper > component), plus the server context, which the browser reads from the page instead.
 */
export function createRenderer({ components, wrapper }: RendererOptions): Renderer {
  const registry = new Map<string, ComponentType<Record<string, unknown>>>();
  for (const [path, module] of Object.entries(components)) {
    const component = (module as { default?: ComponentType<Record<string, unknown>> } | undefined)?.default;
    if (component) {
      registry.set(toIslandName(path), component);
    }
  }
  const Wrapper = wrapper ?? Fragment;

  return ({ component, props, context = {} }) => {
    const Component = registry.get(component);
    if (!Component) {
      const known = Array.from(registry.keys()).sort().join(', ') || '(none)';
      throw new Error(`No component file for "${component}". Known components: ${known}`);
    }
    return renderToString(
      createElement(ServerContextProvider, { value: context }, createElement(Wrapper, null, createElement(Component, props))),
    );
  };
}
