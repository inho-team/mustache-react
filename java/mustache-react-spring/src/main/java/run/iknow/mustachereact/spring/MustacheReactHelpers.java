package run.iknow.mustachereact.spring;

/**
 * The {@code mr} object in Mustache templates: asset tags for pages that contain {@code {{#react}}} islands.
 *
 * <pre>
 * &lt;head&gt; ... {{{mr.head}}} &lt;/head&gt;
 * &lt;body&gt; ... {{#react}}OrderChart orders{{/react}} ... {{{mr.scripts}}} &lt;/body&gt;
 * </pre>
 *
 * <p>{@code mr.head} holds the entry's stylesheets and preloads (or, in development, the Vite dev server client). {@code mr.scripts} must come after the last island:
 * it adds the stylesheets and preloads of the islands used on the page, then the entry script that mounts them.
 */
public class MustacheReactHelpers {

    private final FrontendAssets assets;
    private final ServerContextFactory serverContext;

    /**
     * @param assets produces the asset tags
     * @param serverContext writes the server context script
     */
    public MustacheReactHelpers(FrontendAssets assets, ServerContextFactory serverContext) {
        this.assets = assets;
        this.serverContext = serverContext;
    }

    /**
     * @return stylesheet and preload tags of the client entry, for {@code <head>}
     */
    public String getHead() {
        return assets.viewHead();
    }

    /**
     * @return stylesheet and preload tags of the islands rendered so far, the server context, then the entry script
     */
    public String getScripts() {
        IslandRequestState state = IslandRequestState.current();
        // The same context the islands were rendered with on the server, so that hydration matches.
        String contextJson = state.contextJson(() -> serverContext.json(null));
        return ServerContextFactory.scriptTagFor(contextJson) + assets.viewScripts(state.components());
    }
}
