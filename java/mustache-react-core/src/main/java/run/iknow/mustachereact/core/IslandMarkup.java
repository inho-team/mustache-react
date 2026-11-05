package run.iknow.mustachereact.core;

import java.util.regex.Pattern;

/**
 * Writes the HTML for one island: the element React mounts into, and the props script next to it.
 *
 * <pre>{@code
 * <div data-mr-island="pages/orders/OrderList" data-mr-id="mr-0"></div>
 * <script type="application/json" data-mr-props="mr-0">{"orders":[...]}</script>
 * }</pre>
 */
public final class IslandMarkup {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_$][A-Za-z0-9_$.\\-]*(/[A-Za-z0-9_$][A-Za-z0-9_$.\\-]*)*");

    private IslandMarkup() {
    }

    /**
     * Checks that a component name is a relative path such as {@code pages/orders/OrderList}.
     *
     * @param name the component name
     * @return the same name
     * @throws IllegalArgumentException if the name contains characters other than letters, digits, {@code _ $ . -}
     *         and {@code /} separators, or contains {@code ..}
     */
    public static String requireValidName(String name) {
        if (name == null || !NAME.matcher(name).matches() || name.contains("..")) {
            throw new IllegalArgumentException("Invalid component name '" + name
                    + "'. Use a path relative to the frontend directory without extension, such as 'orders/OrderList'.");
        }
        return name;
    }

    /**
     * Renders an island.
     *
     * @param name component name relative to the frontend directory, e.g. {@code pages/orders/OrderList}
     * @param id id unique within the page, linking the element to its props
     * @param propsJson props as a JSON object
     * @param ssrHtml HTML rendered on the server, or {@code null} to render in the browser only
     * @return the island HTML
     */
    public static String render(String name, String id, String propsJson, String ssrHtml) {
        requireValidName(name);
        String escapedId = Html.escape(id);
        StringBuilder sb = new StringBuilder(propsJson.length() + 160);
        sb.append("<div ").append(Protocol.ISLAND_ATTRIBUTE).append("=\"").append(name).append('"')
                .append(' ').append(Protocol.ISLAND_ID_ATTRIBUTE).append("=\"").append(escapedId).append('"');
        if (ssrHtml != null) {
            sb.append(' ').append(Protocol.SSR_ATTRIBUTE).append("=\"true\">").append(ssrHtml);
        } else {
            sb.append('>');
        }
        sb.append("</div>\n<script type=\"application/json\" ").append(Protocol.PROPS_ATTRIBUTE).append("=\"")
                .append(escapedId).append("\">").append(Html.scriptSafeJson(propsJson)).append("</script>");
        return sb.toString();
    }
}
