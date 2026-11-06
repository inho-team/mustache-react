package run.iknow.mustachereact.core;

/**
 * Small JSON helpers for the protocol between Java and the JS runtime. Application data is written with the
 * application's own JSON library (Jackson) instead.
 */
public final class Json {

    private Json() {
    }

    /**
     * Parses JSON. Objects become {@link java.util.Map}, arrays {@link java.util.List}, numbers {@link Double}.
     *
     * @param json the text
     * @return the value
     * @throws IllegalArgumentException if the text is not JSON
     */
    public static Object parse(String json) {
        return JsonParser.parse(json);
    }

    /**
     * @param value a string
     * @return the string as a JSON string literal, with quotes
     */
    public static String quote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
