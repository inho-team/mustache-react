package run.iknow.mustachereact.spring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * {@code messages}: the listed {@link MessageSource} keys in the request locale ({@code messages.properties},
 * {@code messages_ko.properties}). Only listed keys are exposed.
 */
public class MessagesContextContributor implements ServerContextContributor {

    private final MessageSource messageSource;
    private final List<String> keys;

    /**
     * @param messageSource the application's messages
     * @param keys the keys to expose
     */
    public MessagesContextContributor(MessageSource messageSource, List<String> keys) {
        this.messageSource = messageSource;
        this.keys = new ArrayList<>(keys);
    }

    @Override
    public void contribute(Map<String, Object> context) {
        if (keys.isEmpty()) {
            return;
        }
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> messages = new LinkedHashMap<>();
        for (String key : keys) {
            messages.put(key, messageSource.getMessage(key, null, key, locale));
        }
        context.put("messages", messages);
    }
}
