package run.iknow.mustachereact.core;

import java.util.Collections;
import java.util.List;

/**
 * Asset files a page needs, relative to the build output directory (e.g. {@code assets/OrderList-abc.js}).
 */
public final class PageAssets {

    private final List<String> scripts;
    private final List<String> styles;
    private final List<String> preloads;

    PageAssets(List<String> scripts, List<String> styles, List<String> preloads) {
        this.scripts = Collections.unmodifiableList(scripts);
        this.styles = Collections.unmodifiableList(styles);
        this.preloads = Collections.unmodifiableList(preloads);
    }

    /** @return entry scripts, loaded with {@code <script type="module">} */
    public List<String> getScripts() {
        return scripts;
    }

    /** @return stylesheets, loaded with {@code <link rel="stylesheet">} */
    public List<String> getStyles() {
        return styles;
    }

    /** @return JS chunks to fetch early with {@code <link rel="modulepreload">} */
    public List<String> getPreloads() {
        return preloads;
    }

    /**
     * Renders the tags that belong in {@code <head>}: stylesheets and module preloads.
     *
     * @param publicPath URL path the assets are served from, e.g. {@code /mustache-react/}
     * @return HTML tags, one per line
     */
    public String headTags(String publicPath) {
        StringBuilder sb = new StringBuilder();
        for (String style : styles) {
            sb.append("<link rel=\"stylesheet\" href=\"").append(Html.escape(url(publicPath, style))).append("\">\n");
        }
        for (String preload : preloads) {
            sb.append("<link rel=\"modulepreload\" href=\"").append(Html.escape(url(publicPath, preload))).append("\">\n");
        }
        return sb.toString();
    }

    /**
     * Renders the entry script tags, placed before {@code </body>}.
     *
     * @param publicPath URL path the assets are served from, e.g. {@code /mustache-react/}
     * @return HTML tags, one per line
     */
    public String scriptTags(String publicPath) {
        StringBuilder sb = new StringBuilder();
        for (String script : scripts) {
            sb.append("<script type=\"module\" src=\"").append(Html.escape(url(publicPath, script))).append("\"></script>\n");
        }
        return sb.toString();
    }

    static String url(String publicPath, String file) {
        String base = publicPath.endsWith("/") ? publicPath : publicPath + "/";
        return base + (file.startsWith("/") ? file.substring(1) : file);
    }
}
