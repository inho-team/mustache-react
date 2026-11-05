package run.iknow.mustachereact.spring;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import run.iknow.mustachereact.core.IslandMarkup;

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
    public static final String ISLANDS_DIRECTORY = "islands/";

    private final PropsSerializer propsSerializer;
    private final Map<String, IslandTag> parsed = new ConcurrentHashMap<>();

    /**
     * @param propsSerializer turns the props into JSON with the application's message converters
     */
    public ReactIslandLambda(PropsSerializer propsSerializer) {
        this.propsSerializer = propsSerializer;
    }

    @Override
    public void execute(Template.Fragment fragment, Writer out) throws IOException {
        String source = fragment.decompile();
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

        String component = ISLANDS_DIRECTORY + tag.component;
        String id = IslandRequestState.current().register(component);
        out.write(IslandMarkup.render(component, id, propsSerializer.serialize(props), null));
    }
}
