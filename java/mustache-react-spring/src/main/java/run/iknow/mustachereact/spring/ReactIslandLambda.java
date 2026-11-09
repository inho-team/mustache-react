package run.iknow.mustachereact.spring;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;


/**
 * The {@code {{#react}}...{{/react}}} section: renders a React component from {@code src/main/frontend/islands}
 * in the middle of a Mustache page.
 *
 * <pre>
 * {{#react}}OrderChart orders{{/react}}
 * {{#orders}}
 *   {{#react}}StatusBadge status orderId=id{{/react}}
 * {{/orders}}
 * </pre>
 *
 * <p>Errors (unknown model name, invalid syntax) fail the rendering with a message naming the section, the same way
 * a broken Mustache template does.
 */
public class ReactIslandLambda implements Mustache.Lambda {

    /** Prefix added to island names: {@code OrderChart} is the file {@code islands/OrderChart.tsx}. */
    public static final String ISLANDS_DIRECTORY = IslandRenderer.ISLANDS_DIRECTORY;

    private final IslandRenderer islands;
    private final Map<String, IslandTag> parsed = new ConcurrentHashMap<>();

    /**
     * @param propsSerializer turns the props into JSON with the application's message converters
     */
    public ReactIslandLambda(PropsSerializer propsSerializer) {
        this(propsSerializer, ServerSideRendering.off(), null);
    }

    /**
     * @param propsSerializer turns the props into JSON with the application's message converters
     * @param ssr server-side rendering of the islands
     * @param serverContext the server context given to server-side rendering
     */
    public ReactIslandLambda(PropsSerializer propsSerializer, ServerSideRendering ssr, ServerContextFactory serverContext) {
        this.islands = new IslandRenderer(propsSerializer, ssr, serverContext, null);
    }

    @Override
    public void execute(Template.Fragment fragment, Writer out) throws IOException {
        String source = fragment.decompile();
        if (HtmlIslandTags.isTag(source)) {
            out.write(renderTag(fragment, source));
            return;
        }
        IslandTag tag = parsed.get(source);
        if (tag == null) {
            tag = IslandTag.parse(source);
            parsed.put(source, tag);
        }

        Map<String, Object> props = new LinkedHashMap<>();
        for (Map.Entry<String, String> prop : tag.props.entrySet()) {
            Object value = ContextPaths.resolve(fragment, prop.getValue());
            if (value == ContextPaths.MISSING) {
                String path = prop.getValue();
                String root = path.split("\\.")[0];
                boolean rootMissing = ContextPaths.resolve(fragment, root) == ContextPaths.MISSING;
                throw new IllegalArgumentException("{{#react}}" + source.trim() + "{{/react}}: '" + path + "' was not "
                        + "found in the model or the enclosing sections."
                        + (rootMissing ? " Add it with model.addAttribute(\"" + root + "\", ...)." : ""));
            }
            props.put(prop.getKey(), value);
        }

        out.write(islands.render(tag.component, props));
    }

    /** {@code <react:Name a="{{path}}" b="text {{x}}" flag />}, see {@link HtmlIslandTags}. */
    private String renderTag(Template.Fragment fragment, String source) {
        HtmlIslandTags.Tag tag = HtmlIslandTags.parse(source);
        // The tag rendered by Mustache gives the text values, with {{...}} filled in and escaped.
        List<String[]> rendered = null;
        Map<String, Object> props = new LinkedHashMap<>();
        for (int i = 0; i < tag.attributes.size(); i++) {
            HtmlIslandTags.Attribute attribute = tag.attributes.get(i);
            Object value;
            if (attribute.flag) {
                value = Boolean.TRUE;
            } else if (attribute.path != null) {
                value = ContextPaths.resolve(fragment, attribute.path);
                if (value == ContextPaths.MISSING) {
                    throw new IllegalArgumentException(source.trim() + ": '" + attribute.path
                            + "' was not found in the model or the enclosing sections.");
                }
            } else {
                if (rendered == null) {
                    String html = fragment.execute();
                    rendered = HtmlIslandTags.attributes(html.substring(html.indexOf(':') + 1).replaceFirst("^\\S+", ""));
                }
                value = HtmlIslandTags.unescape(textValue(rendered, tag, i));
            }
            props.put(attribute.prop, value);
        }
        return islands.render(tag.component, props);
    }

    /** The rendered value of the i-th prop attribute, skipping {@code component} of {@code <react:island>}. */
    private static String textValue(List<String[]> rendered, HtmlIslandTags.Tag tag, int index) {
        int seen = -1;
        for (String[] attribute : rendered) {
            if (tag.named && attribute[0].equals("component")) {
                continue;
            }
            if (++seen == index) {
                return attribute[1] == null ? "" : attribute[1];
            }
        }
        return "";
    }
}
