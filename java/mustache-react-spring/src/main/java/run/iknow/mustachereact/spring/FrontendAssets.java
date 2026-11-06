package run.iknow.mustachereact.spring;

import java.util.Collection;

/**
 * Produces the {@code <link>} and {@code <script>} tags that load the frontend: from the built files
 * ({@link ManifestFrontendAssets}) or from the Vite dev server ({@link DevServerFrontendAssets}).
 */
public interface FrontendAssets {

    /**
     * Tags for the {@code <head>} of a page rendered by {@code @ReactPage}.
     *
     * @param component the page component, e.g. {@code pages/orders/OrderList}
     * @return HTML tags
     */
    String pageHead(String component);

    /**
     * Tags placed before {@code </body>} of a page rendered by {@code @ReactPage}.
     *
     * @param component the page component, e.g. {@code pages/orders/OrderList}
     * @return HTML tags
     */
    String pageScripts(String component);

    /**
     * {@code mr.head} of a Mustache view: tags for {@code <head>}, written before the islands are known.
     *
     * @return HTML tags
     */
    String viewHead();

    /**
     * {@code mr.scripts} of a Mustache view: tags placed after the last island.
     *
     * @param components the island components rendered on the page, e.g. {@code islands/OrderChart}
     * @return HTML tags
     */
    String viewScripts(Collection<String> components);
}
