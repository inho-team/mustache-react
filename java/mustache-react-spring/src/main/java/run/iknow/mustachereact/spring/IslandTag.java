package run.iknow.mustachereact.spring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import run.iknow.mustachereact.core.IslandMarkup;

/**
 * The content of a {@code {{#react}}...{{/react}}} section: a component name followed by props.
 *
 * <pre>
 * {{#react}}OrderChart orders{{/react}}                 orders = model value "orders"
 * {{#react}}UserBadge user=currentUser{{/react}}        user   = model value "currentUser"
 * {{#react}}StatusBadge status id=order.id{{/react}}    id     = value at path "order.id"
 * </pre>
 *
 * <p>Tokens are separated by whitespace, including line breaks.
 */
final class IslandTag {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern PATH = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");

    final String component;
    /** Prop name to path, in the order written. */
    final Map<String, String> props;

    private IslandTag(String component, Map<String, String> props) {
        this.component = component;
        this.props = Collections.unmodifiableMap(props);
    }

    static IslandTag parse(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty {{#react}} section. Write the component name, e.g. "
                    + "{{#react}}OrderChart orders{{/react}}");
        }
        String[] tokens = trimmed.split("\\s+");
        String component;
        try {
            component = IslandMarkup.requireValidName(tokens[0]);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid component name '" + tokens[0] + "' in {{#react}}" + trimmed
                    + "{{/react}}. Use the file name under src/main/frontend/islands without extension.", ex);
        }
        Map<String, String> props = new LinkedHashMap<>();
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            int eq = token.indexOf('=');
            String name = eq < 0 ? token : token.substring(0, eq);
            String path = eq < 0 ? token : token.substring(eq + 1);
            if (!IDENTIFIER.matcher(name).matches() || !PATH.matcher(path).matches()
                    || (eq < 0 && ContextPaths.THIS.equals(name))) {
                throw new IllegalArgumentException("Invalid prop '" + token + "' in {{#react}}" + trimmed
                        + "{{/react}}. Write a model name ('orders') or name=path ('user=currentUser', 'id=order.id', "
                        + "'item=this').");
            }
            if (props.put(name, path) != null) {
                throw new IllegalArgumentException("Prop '" + name + "' is given twice in {{#react}}" + trimmed + "{{/react}}");
            }
        }
        return new IslandTag(component, props);
    }
}
