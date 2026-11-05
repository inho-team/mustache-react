package run.iknow.mustachereact.core;

/**
 * HTML markers shared between the server renderer and the browser runtime.
 *
 * <p>These values must stay identical to {@code js/client/src/protocol.ts}.
 */
public final class Protocol {

    /** Attribute on the island root element. Its value is the component name, e.g. {@code orders/OrderList}. */
    public static final String ISLAND_ATTRIBUTE = "data-mr-island";

    /** Attribute linking an island root to its props script. */
    public static final String ISLAND_ID_ATTRIBUTE = "data-mr-id";

    /** Attribute set to {@code "true"} when the island HTML was rendered on the server and must be hydrated. */
    public static final String SSR_ATTRIBUTE = "data-mr-ssr";

    /** Attribute on the {@code <script type="application/json">} element holding an island's props. */
    public static final String PROPS_ATTRIBUTE = "data-mr-props";

    /** Id of the {@code <script type="application/json">} element holding the server context (CSRF, user, locale). */
    public static final String CONTEXT_SCRIPT_ID = "mr-context";

    private Protocol() {
    }
}
