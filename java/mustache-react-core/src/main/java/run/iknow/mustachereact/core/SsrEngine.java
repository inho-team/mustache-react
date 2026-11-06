package run.iknow.mustachereact.core;

/**
 * Renders a component to HTML on the server, for the browser to hydrate.
 *
 * <p>The Spring integration provides an HTTP engine that calls the Node.js SSR server (a sidecar started by the
 * application, or the Vite dev server in development). Other engines, for example GraalJS, can implement this.
 */
@FunctionalInterface
public interface SsrEngine {

    /**
     * @param component component path, e.g. {@code pages/orders/OrderList}
     * @param propsJson the props as a JSON object
     * @param contextJson the server context of the page as a JSON object
     * @return the HTML of the component
     * @throws SsrException if rendering fails or the engine is unavailable
     */
    String render(String component, String propsJson, String contextJson) throws SsrException;
}
