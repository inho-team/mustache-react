package run.iknow.mustachereact.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * {@code messages}: the listed {@link MessageSource} keys in the request locale ({@code messages.properties},
 * {@code messages_en.properties}). Only listed keys are written into the page.
 *
 * <p>An entry is a key ({@code app.name}), a prefix pattern ({@code orders.*}: every key that starts with
 * {@code orders.}), or {@code *} (every key). Patterns are expanded with the keys of the message bundles.
 *
 * <p>Messages are written as they are in the bundle; {@code {0}} placeholders are filled in by the client
 * ({@code t('orders.count', 3)}), following {@link java.text.MessageFormat} as Spring does.
 */
public class MessagesContextContributor implements ServerContextContributor {

    private final MessageSource messageSource;
    private final List<String> keys = new ArrayList<>();
    private final List<String> prefixes = new ArrayList<>();
    private final boolean all;
    private final Supplier<? extends Collection<String>> bundleKeys;
    private volatile Set<String> expanded;

    /**
     * @param messageSource the application's messages
     * @param keys the keys to expose; no patterns
     */
    public MessagesContextContributor(MessageSource messageSource, List<String> keys) {
        this(messageSource, keys, Collections::<String>emptySet);
    }

    /**
     * @param messageSource the application's messages
     * @param entries keys and patterns ({@code orders.*}, {@code *}) to expose
     * @param bundleKeys the keys defined in the message bundles, to expand the patterns, see {@link MessageKeys}
     */
    public MessagesContextContributor(MessageSource messageSource, List<String> entries,
            Supplier<? extends Collection<String>> bundleKeys) {
        this.messageSource = messageSource;
        this.bundleKeys = bundleKeys;
        boolean any = false;
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.equals("*")) {
                any = true;
            } else if (trimmed.endsWith("*")) {
                prefixes.add(trimmed.substring(0, trimmed.length() - 1));
            } else if (!trimmed.isEmpty()) {
                keys.add(trimmed);
            }
        }
        this.all = any;
    }

    /**
     * @return the keys written into every page, with the patterns expanded
     */
    public Set<String> keys() {
        Set<String> result = expanded;
        if (result == null) {
            result = new LinkedHashSet<>(keys);
            if (all || !prefixes.isEmpty()) {
                for (String key : bundleKeys.get()) {
                    if (all || startsWithAny(key)) {
                        result.add(key);
                    }
                }
            }
            result = Collections.unmodifiableSet(result);
            expanded = result;
        }
        return result;
    }

    private boolean startsWithAny(String key) {
        for (String prefix : prefixes) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void contribute(Map<String, Object> context) {
        Set<String> exposed = keys();
        if (exposed.isEmpty()) {
            return;
        }
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> messages = new LinkedHashMap<>();
        for (String key : exposed) {
            messages.put(key, messageSource.getMessage(key, null, key, locale));
        }
        context.put("messages", messages);
    }
}
