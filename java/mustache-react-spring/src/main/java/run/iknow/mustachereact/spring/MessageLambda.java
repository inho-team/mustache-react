package run.iknow.mustachereact.spring;

import java.io.IOException;
import java.io.Writer;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.util.HtmlUtils;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

/**
 * {@code {{#mr.t}}key arg ...{{/mr.t}}}: a message in the request locale, HTML-escaped. Arguments are model paths,
 * looked up like island props, and fill {@code {0}}, {@code {1}}, ... as in Spring's {@code MessageSource}.
 *
 * <pre>
 * &lt;h1&gt;{{#mr.t}}dashboard.title{{/mr.t}}&lt;/h1&gt;
 * &lt;p&gt;{{#mr.t}}orders.count orders.size{{/mr.t}}&lt;/p&gt;
 * </pre>
 *
 * <p>A key without a message is written as it is.
 */
public class MessageLambda implements Mustache.Lambda {

    private final MessageSource messageSource;

    /**
     * @param messageSource the application's messages
     */
    public MessageLambda(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public void execute(Template.Fragment fragment, Writer out) throws IOException {
        String source = fragment.decompile().trim();
        String[] parts = source.isEmpty() ? new String[0] : source.split("\\s+");
        if (parts.length == 0) {
            throw new IllegalArgumentException("{{#mr.t}}{{/mr.t}}: a message key is missing, e.g. {{#mr.t}}app.name{{/mr.t}}");
        }
        Object[] args = new Object[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            Object value = ContextPaths.resolve(fragment, parts[i]);
            if (value == ContextPaths.MISSING) {
                throw new IllegalArgumentException("{{#mr.t}}" + source + "{{/mr.t}}: '" + parts[i]
                        + "' was not found in the model or the enclosing sections.");
            }
            args[i - 1] = value;
        }
        String message = messageSource.getMessage(parts[0], args.length == 0 ? null : args, parts[0],
                LocaleContextHolder.getLocale());
        out.write(HtmlUtils.htmlEscape(message == null ? parts[0] : message));
    }
}
