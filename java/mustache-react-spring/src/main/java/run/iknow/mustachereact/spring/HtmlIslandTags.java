package run.iknow.mustachereact.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Islands written as HTML tags in Mustache templates:
 *
 * <pre>
 * &lt;react:head /&gt;                                            → {{{mr.head}}}
 * &lt;react:StatusBadge status="{{status}}" order-id="{{id}}" /&gt; → an island, props {status, orderId}
 * &lt;react:island component="charts/OrderChart" orders="{{orders}}" /&gt;
 * &lt;react:scripts /&gt;                                         → {{{mr.scripts}}}
 * </pre>
 *
 * <p>{@link #expand} rewrites the tags when a template is compiled: an island tag becomes a
 * {@code {{#react}}...{{/react}}} section around the tag, which {@link ReactIslandLambda} reads with {@link #parse}.
 * An attribute whose value is exactly {@code {{path}}} passes the model value itself (a list, a DTO, a number);
 * any other value is text, with {@code {{...}}} inside it filled in; an attribute without a value is {@code true}.
 * {@code order-id} becomes the prop {@code orderId}.
 */
public final class HtmlIslandTags {

    private static final Pattern ATTRIBUTE = Pattern.compile(
            "([A-Za-z_][-A-Za-z0-9_.:]*)(?:\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'))?");
    private static final Pattern MODEL_VALUE = Pattern.compile("\\{\\{\\{?\\s*([A-Za-z_$][A-Za-z0-9_$.]*)\\s*}?}}");

    private HtmlIslandTags() {
    }

    /**
     * Rewrites the island tags of a template.
     *
     * @param template Mustache template source
     * @param prefix tag prefix, {@code react} for {@code <react:Name>}
     * @param section name of the island section ({@code react})
     * @param helpers name of the helpers ({@code mr})
     * @return the template with plain Mustache sections in place of the tags
     */
    public static String expand(String template, String prefix, String section, String helpers) {
        if (template.indexOf("<" + prefix + ":") < 0) {
            return template;
        }
        // <react:Name ... /> or <react:Name ...></react:Name>; attribute values may contain {{...}} but not '>'.
        Pattern tag = Pattern.compile("<" + Pattern.quote(prefix) + ":([A-Za-z0-9_$./-]+)((?:\\s+[^>]*?)?)\\s*(?:/>|>\\s*</"
                + Pattern.quote(prefix) + ":\\1\\s*>)");
        Matcher matcher = tag.matcher(template);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement;
            if (name.equals("head")) {
                replacement = "{{{" + helpers + ".head}}}";
            } else if (name.equals("scripts")) {
                replacement = "{{{" + helpers + ".scripts}}}";
            } else {
                replacement = "{{#" + section + "}}" + matcher.group() + "{{/" + section + "}}";
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** @return whether a section body is an island tag rather than {@code Name prop name=path} */
    static boolean isTag(String body) {
        return body.trim().startsWith("<");
    }

    /** A parsed island tag. */
    static final class Tag {
        final String component;
        /** {@code <react:island component="...">} */
        final boolean named;
        final List<Attribute> attributes;

        Tag(String component, boolean named, List<Attribute> attributes) {
            this.component = component;
            this.named = named;
            this.attributes = Collections.unmodifiableList(attributes);
        }
    }

    /** One attribute: the prop name, and either a model path or text. */
    static final class Attribute {
        final String prop;
        /** Model path when the value is exactly {@code {{path}}}, else {@code null}. */
        final String path;
        /** Whether the attribute has no value ({@code <react:Toggle open />}). */
        final boolean flag;

        Attribute(String prop, String path, boolean flag) {
            this.prop = prop;
            this.path = path;
            this.flag = flag;
        }
    }

    /**
     * @param source the tag, as written in the template
     * @return its component and attributes
     */
    static Tag parse(String source) {
        String text = source.trim();
        int nameStart = text.indexOf(':') + 1;
        int nameEnd = nameStart;
        while (nameEnd < text.length() && !Character.isWhitespace(text.charAt(nameEnd)) && text.charAt(nameEnd) != '/'
                && text.charAt(nameEnd) != '>') {
            nameEnd++;
        }
        String name = text.substring(nameStart, nameEnd);
        boolean named = name.equals("island");
        String component = named ? null : componentName(name);
        List<Attribute> attributes = new ArrayList<>();
        for (String[] attribute : attributes(text.substring(nameEnd))) {
            if (named && attribute[0].equals("component")) {
                component = attribute[1];
                continue;
            }
            Matcher model = attribute[1] == null ? null : MODEL_VALUE.matcher(attribute[1].trim());
            attributes.add(new Attribute(propName(attribute[0]), model != null && model.matches() ? model.group(1) : null,
                    attribute[1] == null));
        }
        if (component == null || component.isEmpty()) {
            throw new IllegalArgumentException(text + ": write component=\"dir/Name\"");
        }
        return new Tag(component, named, attributes);
    }

    /**
     * @param tag the tag text, after its name
     * @return name and value (null without a value) of each attribute, in order
     */
    static List<String[]> attributes(String tag) {
        String text = tag.replaceAll("\\s*/?>\\s*(</[^>]*>)?\\s*$", "");
        List<String[]> result = new ArrayList<>();
        Matcher matcher = ATTRIBUTE.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            result.add(new String[] {matcher.group(1), value});
        }
        return result;
    }

    /** {@code order-id} and {@code orderId} are the prop {@code orderId}. */
    static String propName(String attribute) {
        StringBuilder name = new StringBuilder(attribute.length());
        boolean upper = false;
        for (char c : attribute.toCharArray()) {
            if (c == '-' || c == '_') {
                upper = name.length() > 0;
            } else {
                name.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return name.toString();
    }

    /** {@code OrderChart} as written; {@code order-chart} becomes {@code OrderChart}. */
    static String componentName(String name) {
        if (name.indexOf('-') < 0) {
            return name;
        }
        String camel = propName(name);
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /** Text from a rendered attribute value: the entities Mustache's escaping writes, undone. */
    static String unescape(String html) {
        return html.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&#x27;", "'").replace("&#x3D;", "=").replace("&#x60;", "`").replace("&amp;", "&");
    }
}
