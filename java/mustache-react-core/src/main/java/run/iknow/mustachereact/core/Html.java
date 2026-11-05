package run.iknow.mustachereact.core;

/**
 * HTML escaping helpers.
 */
public final class Html {

    private Html() {
    }

    /**
     * Escapes text for use in HTML element content or in a double-quoted attribute value.
     *
     * @param text the text, may be {@code null}
     * @return the escaped text, empty for {@code null}
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Makes a JSON document safe to embed in {@code <script type="application/json">}.
     *
     * <p>Replaces {@code <}, {@code >}, {@code &}, U+2028 and U+2029 with their {@code \}{@code uXXXX} escapes, so
     * that a value such as {@code "</script>"} cannot close the script element or open an HTML comment.
     * The result is still valid JSON with the same meaning; {@code JSON.parse} restores the original characters.
     * Only use this on JSON text, where these characters can appear only inside strings.
     *
     * @param json a JSON document
     * @return the JSON document, safe to place between {@code <script>} tags
     */
    public static String scriptSafeJson(String json) {
        StringBuilder sb = null;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            String replacement;
            switch (c) {
                case '<': replacement = "\\u003c"; break;
                case '>': replacement = "\\u003e"; break;
                case '&': replacement = "\\u0026"; break;
                case ' ': replacement = "\\u2028"; break;
                case ' ': replacement = "\\u2029"; break;
                default: replacement = null;
            }
            if (replacement != null) {
                if (sb == null) {
                    sb = new StringBuilder(json.length() + 32).append(json, 0, i);
                }
                sb.append(replacement);
            } else if (sb != null) {
                sb.append(c);
            }
        }
        return sb == null ? json : sb.toString();
    }
}
