package run.iknow.mustachereact.spring;

import java.util.Map;

import run.iknow.mustachereact.core.IslandMarkup;

/**
 * Renders React islands into Mustache templates: the island markup with its props, the
 * server-rendered HTML when SSR is on, and the asset tags of the page. Islands of one request share a counter and
 * the list of components, so {@link #scripts()} preloads exactly the islands on the page.
 */
public class IslandRenderer {

    /** Prefix added to island names: {@code OrderChart} is the file {@code islands/OrderChart.tsx}. */
    public static final String ISLANDS_DIRECTORY = "islands/";

    private final PropsSerializer propsSerializer;
    private final ServerSideRendering ssr;
    private final ServerContextFactory serverContext;
    private final FrontendAssets assets;
    private volatile IslandDeclarations declarations;

    /**
     * @param propsSerializer turns props into JSON
     * @param ssr renders islands on the server, or not
     * @param serverContext the server context of the page; {@code null} for none
     * @param assets asset tags for {@link #head()} and {@link #scripts()}; {@code null} if those are not used
     */
    public IslandRenderer(PropsSerializer propsSerializer, ServerSideRendering ssr, ServerContextFactory serverContext,
            FrontendAssets assets) {
        this.propsSerializer = propsSerializer;
        this.ssr = ssr == null ? ServerSideRendering.off() : ssr;
        this.serverContext = serverContext;
        this.assets = assets;
    }

    /**
     * @param declarations the props of {@code @ReactIsland} classes, checked on every island; by default read from
     *     the class path ({@link IslandDeclarations#RESOURCE})
     * @return this renderer
     */
    public IslandRenderer declarations(IslandDeclarations declarations) {
        this.declarations = declarations;
        return this;
    }

    private IslandDeclarations declarations() {
        IslandDeclarations result = declarations;
        if (result == null) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            result = IslandDeclarations.load(loader != null ? loader : IslandRenderer.class.getClassLoader());
            declarations = result;
        }
        return result;
    }

    /**
     * @param name component under {@code src/main/frontend/islands}, without extension, e.g. {@code OrderChart} or
     *     {@code charts/OrderChart}
     * @param props the component's props
     * @return the island markup to write into the page
     */
    public String render(String name, Map<String, Object> props) {
        String component = ISLANDS_DIRECTORY + IslandMarkup.requireValidName(name);
        declarations().check(name, props.keySet());
        IslandRequestState state = IslandRequestState.current();
        String id = state.register(component);
        String propsJson = propsSerializer.serialize(props);
        String ssrHtml = ssr.render(component, propsJson, () -> contextJson(state));
        return IslandMarkup.render(component, id, propsJson, ssrHtml);
    }

    /**
     * @return stylesheet and preload tags of the client entry, for {@code <head>}
     */
    public String head() {
        return assets.viewHead();
    }

    /**
     * @return stylesheet and preload tags of the islands rendered so far, the server context, then the entry
     *     script; goes after the last island
     */
    public String scripts() {
        IslandRequestState state = IslandRequestState.current();
        // The same context the islands were rendered with on the server, so that hydration matches.
        return ServerContextFactory.scriptTagFor(contextJson(state)) + assets.viewScripts(state.components());
    }

    private String contextJson(IslandRequestState state) {
        return state.contextJson(() -> serverContext == null ? "{}" : serverContext.json(null));
    }
}
