package run.iknow.mustachereact.spring;

import java.util.Collection;

import run.iknow.mustachereact.core.PageAssets;

/**
 * Loads the built frontend, using the Vite manifest to find the hashed files and to preload the page's chunks.
 */
public class ManifestFrontendAssets implements FrontendAssets {

    private final ViteManifestSource manifest;
    private final String publicPath;

    /**
     * @param manifest the Vite manifest
     * @param publicPath URL path the built assets are served from, e.g. {@code /mustache-react/}
     */
    public ManifestFrontendAssets(ViteManifestSource manifest, String publicPath) {
        this.manifest = manifest;
        this.publicPath = publicPath;
    }

    @Override
    public String pageHead(String component) {
        return manifest.get().assetsFor(component).headTags(publicPath);
    }

    @Override
    public String pageScripts(String component) {
        return manifest.get().assetsFor(component).scriptTags(publicPath);
    }

    @Override
    public String viewHead() {
        return manifest.get().entryAssets().headTags(publicPath);
    }

    @Override
    public String viewScripts(Collection<String> components) {
        PageAssets head = manifest.get().entryAssets();
        PageAssets islands = manifest.get().assetsForComponents(components).without(head);
        return islands.headTags(publicPath) + islands.scriptTags(publicPath);
    }
}
