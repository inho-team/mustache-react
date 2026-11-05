package run.iknow.mustachereact.core;

import java.util.ArrayList;
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
     * Returns the styles and preloads of these assets that are not in {@code other}, keeping the scripts.
     *
     * <p>Used on Mustache pages: {@code mr.head} is written before the islands are known and holds the entry's
     * assets; {@code mr.scripts} adds what the islands need on top of that.
     *
     * @param other assets already referenced by the page
     * @return the remaining assets
     */
    public PageAssets without(PageAssets other) {
        List<String> remainingStyles = new ArrayList<>(styles);
        remainingStyles.removeAll(other.styles);
        List<String> remainingPreloads = new ArrayList<>(preloads);
        remainingPreloads.removeAll(other.preloads);
        return new PageAssets(new ArrayList<>(scripts), remainingStyles, remainingPreloads);
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
