import { createElement, Fragment, type ComponentType, type ReactNode } from 'react';
import { createRoot, hydrateRoot } from 'react-dom/client';
import { Protocol } from './protocol.js';

/** A module whose default export is a React component (a file under `pages/` or `islands/`). */
export interface ComponentModule {
  default: ComponentType<any>;
}

/** Lazy module loaders keyed by file path, as returned by Vite's `import.meta.glob`. */
export type ComponentLoaders = Record<string, () => Promise<unknown>>;

export interface BootstrapOptions {
  /**
   * Component wrapping every island, e.g. to provide a theme or a query client.
   * Comes from `src/main/frontend/app.tsx` when that file exists.
   */
  wrapper?: ComponentType<{ children?: ReactNode }>;
  /** Element to search for islands. Default: `document`. */
  root?: ParentNode;
}

/**
 * Turns `import.meta.glob` keys into island names.
 *
 * `/pages/orders/OrderList.tsx` and `./pages/orders/OrderList.tsx` both become `pages/orders/OrderList`,
 * which is the value the server writes into `data-mr-island`.
 */
export function toIslandName(path: string): string {
  return path.replace(/^\.?\//, '').replace(/\.[cm]?[jt]sx?$/, '');
}

/** Reads the props of an island from its `<script type="application/json">` element. */
export function readProps(element: Element, scope: ParentNode = document): Record<string, unknown> {
  const id = element.getAttribute(Protocol.ISLAND_ID_ATTRIBUTE);
  if (!id) {
    return {};
  }
  const script = scope.querySelector(`script[${Protocol.PROPS_ATTRIBUTE}="${CSS.escape(id)}"]`);
  const text = script?.textContent?.trim();
  if (!text) {
    return {};
  }
  const value: unknown = JSON.parse(text);
  return value !== null && typeof value === 'object' ? (value as Record<string, unknown>) : {};
}

/**
 * Finds every island rendered by the server and mounts its React component.
 *
 * Islands rendered on the server (`data-mr-ssr="true"`) are hydrated; the others are rendered from scratch.
 * One failing island does not stop the others.
 *
 * @returns a promise that settles when every island has been processed
 */
export function bootstrap(loaders: ComponentLoaders, options: BootstrapOptions = {}): Promise<void> {
  const scope = options.root ?? document;
  const registry = new Map<string, () => Promise<unknown>>();
  for (const [path, loader] of Object.entries(loaders)) {
    registry.set(toIslandName(path), loader);
  }

  const elements = Array.from(scope.querySelectorAll<HTMLElement>(`[${Protocol.ISLAND_ATTRIBUTE}]`));
  return Promise.all(
    elements.map((element) =>
      mountIsland(element, registry, options, scope).catch((error: unknown) => {
        console.error(`[mustache-react] Failed to mount island "${element.getAttribute(Protocol.ISLAND_ATTRIBUTE)}"`, error);
      }),
    ),
  ).then(() => undefined);
}

async function mountIsland(
  element: HTMLElement,
  registry: Map<string, () => Promise<unknown>>,
  options: BootstrapOptions,
  scope: ParentNode,
): Promise<void> {
  const name = element.getAttribute(Protocol.ISLAND_ATTRIBUTE) ?? '';
  const loader = registry.get(name);
  if (!loader) {
    const known = Array.from(registry.keys()).sort().join(', ') || '(none)';
    throw new Error(`No component file for "${name}". Known components: ${known}`);
  }

  const module = (await loader()) as Partial<ComponentModule>;
  const Component = module.default;
  if (Component == null || (typeof Component !== 'function' && typeof Component !== 'object')) {
    throw new Error(`"${name}" has no default export. Export the component with "export default".`);
  }

  const props = readProps(element, scope);
  const Wrapper = options.wrapper ?? Fragment;
  const tree = createElement(Wrapper, null, createElement(Component, props));

  if (element.getAttribute(Protocol.SSR_ATTRIBUTE) === 'true') {
    hydrateRoot(element, tree);
  } else {
    createRoot(element).render(tree);
  }
}
