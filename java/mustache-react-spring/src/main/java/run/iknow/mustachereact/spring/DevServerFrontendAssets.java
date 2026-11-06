package run.iknow.mustachereact.spring;

import java.util.Collection;

import run.iknow.mustachereact.core.ViteDevServer;

/**
 * Loads the frontend from a running Vite dev server, with hot module replacement. No build or manifest is needed;
 * component chunks are not preloaded, as the dev server serves modules one by one.
 */
public class DevServerFrontendAssets implements FrontendAssets {

    private final ViteDevServer server;

    /**
     * @param server the dev server
     */
    public DevServerFrontendAssets(ViteDevServer server) {
        this.server = server;
    }

    @Override
    public String pageHead(String component) {
        return server.headTags();
    }

    @Override
    public String pageScripts(String component) {
        return server.scriptTags();
    }

    @Override
    public String viewHead() {
        return server.headTags();
    }

    @Override
    public String viewScripts(Collection<String> components) {
        return server.scriptTags();
    }
}
