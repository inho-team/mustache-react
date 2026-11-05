package run.iknow.mustachereact.spring;

import run.iknow.mustachereact.core.PageAssets;

/**
 * The {@code mr} object in Mustache templates: asset tags for pages that contain {@code {{#react}}} islands.
 *
 * <pre>
 * &lt;head&gt; ... {{{mr.head}}} &lt;/head&gt;
 * &lt;body&gt; ... {{#react}}OrderChart orders{{/react}} ... {{{mr.scripts}}} &lt;/body&gt;
 * </pre>
 *
 * <p>{@code mr.head} holds the entry's stylesheets and preloads. {@code mr.scripts} must come after the last island:
 * it adds the stylesheets and preloads of the islands used on the page, then the entry script that mounts them.
 */
public class MustacheReactHelpers {

    private final ViteManifestSource manifest;
    private final String publicPath;

    /**
     * @param manifest the Vite manifest
     * @param publicPath URL path the built assets are served from, e.g. {@code /mustache-react/}
     */
    public MustacheReactHelpers(ViteManifestSource manifest, String publicPath) {
        this.manifest = manifest;
        this.publicPath = publicPath;
    }

    /**
     * @return stylesheet and preload tags of the client entry, for {@code <head>}
     */
    public String getHead() {
        return manifest.get().entryAssets().headTags(publicPath);
    }

    /**
     * @return stylesheet and preload tags of the islands rendered so far, then the entry script
     */
    public String getScripts() {
        PageAssets head = manifest.get().entryAssets();
        PageAssets islands = manifest.get().assetsForComponents(IslandRequestState.current().components()).without(head);
        return islands.headTags(publicPath) + islands.scriptTags(publicPath);
    }
}
