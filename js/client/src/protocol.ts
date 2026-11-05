/**
 * HTML markers shared between the server renderer and the browser runtime.
 *
 * These values must stay identical to
 * `java/mustache-react-core/src/main/java/run/iknow/mustachereact/core/Protocol.java`.
 */
export const Protocol = {
  /** Attribute on the island root element. Its value is the component name, e.g. `orders/OrderList`. */
  ISLAND_ATTRIBUTE: 'data-mr-island',
  /** Attribute linking an island root to its props script. */
  ISLAND_ID_ATTRIBUTE: 'data-mr-id',
  /** Attribute set to `"true"` when the island HTML was rendered on the server and must be hydrated. */
  SSR_ATTRIBUTE: 'data-mr-ssr',
  /** Attribute on the `<script type="application/json">` element holding an island's props. */
  PROPS_ATTRIBUTE: 'data-mr-props',
  /** Id of the `<script type="application/json">` element holding the server context. */
  CONTEXT_SCRIPT_ID: 'mr-context',
} as const;
