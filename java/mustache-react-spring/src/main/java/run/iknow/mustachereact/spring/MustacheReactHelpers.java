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

    /**
     * @param assets produces the asset tags
     */
    public MustacheReactHelpers(FrontendAssets assets) {
        this.assets = assets;
    }

    /**
     * @return stylesheet and preload tags of the client entry, for {@code <head>}
     */
    public String getHead() {
        return assets.viewHead();
    }

    /**
     * @return stylesheet and preload tags of the islands rendered so far, then the entry script
     */
    public String getScripts() {
        return assets.viewScripts(IslandRequestState.current().components());
    }
}
