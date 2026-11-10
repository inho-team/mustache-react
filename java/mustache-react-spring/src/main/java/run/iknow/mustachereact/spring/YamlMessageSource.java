package run.iknow.mustachereact.spring;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Messages from YAML files: {@code messages.yml} (the default language) and {@code messages_en.yml},
 * {@code messages_en_US.yml}, ... next to it. Nested keys are joined with dots, so
 *
 * <pre>
 * orders:
 *   title: 주문 목록
 *   count: 주문 {0}건
 * </pre>
 *
 * defines {@code orders.title} and {@code orders.count}, exactly as {@code messages.properties} would. Files are read
 * as UTF-8; {@code .yaml} works as well as {@code .yml}. A locale is looked up like a resource bundle
 * ({@code ko_KR} → {@code ko} → the default file); the system locale plays no part.
 *
 * <p>Formatting follows Spring's other message sources: {@code {0}} arguments with {@link MessageFormat}, and a
 * message without arguments as written unless {@link #setAlwaysUseMessageFormat} is on.
 */
public class YamlMessageSource extends AbstractMessageSource {

    private final ResourcePatternResolver resolver;
    private final List<String> basenames;
    private final Map<Locale, Properties> cache = new ConcurrentHashMap<>();
    private final Map<String, Map<Locale, MessageFormat>> formats = new ConcurrentHashMap<>();
    private volatile long cacheMillis = -1;
    private volatile long loadedAt;

    /**
     * @param resolver finds the files
     * @param basenames base names such as {@code messages} or {@code i18n/messages}, as in
     *     {@code spring.messages.basename}; a {@code classpath:} prefix is allowed
     */
    public YamlMessageSource(ResourcePatternResolver resolver, List<String> basenames) {
        this.resolver = resolver == null ? new PathMatchingResourcePatternResolver() : resolver;
        List<String> names = new ArrayList<>();
        for (String basename : basenames) {
            String name = basename.trim().replaceFirst("^classpath\\*?:", "").replaceFirst("^/+", "");
            if (!name.isEmpty()) {
                names.add(name.replace('.', '/'));
            }
        }
        this.basenames = Collections.unmodifiableList(names);
    }

    /**
     * @param millis how long loaded files are kept; {@code -1} (the default) keeps them, {@code 0} reads them for
     *     every lookup. Development mode uses a short time so that edits show up without a restart.
     */
    public void setCacheMillis(long millis) {
        this.cacheMillis = millis;
    }

    /** @return how long loaded files are kept, see {@link #setCacheMillis} */
    public long getCacheMillis() {
        return cacheMillis;
    }

    /**
     * @param basenames base names, see the constructor
     * @param resolver finds the files
     * @return whether any {@code <basename>.yml} or {@code .yaml} exists
     */
    public static boolean exists(ResourcePatternResolver resolver, List<String> basenames) {
        YamlMessageSource probe = new YamlMessageSource(resolver, basenames);
        for (String basename : probe.basenames) {
            for (String extension : new String[] {".yml", ".yaml"}) {
                if (!probe.resources(basename + extension).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        return messages(locale).getProperty(code);
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        String message = resolveCodeWithoutArguments(code, locale);
        if (message == null) {
            return null;
        }
        // Cleared together with the files when they are read again.
        return formats.computeIfAbsent(code, c -> new ConcurrentHashMap<>())
                .computeIfAbsent(locale, l -> createMessageFormat(message, l));
    }

    /** All messages for a locale, the more specific files winning. */
    Properties messages(Locale locale) {
        Locale key = locale == null ? Locale.ROOT : locale;
        if (cacheMillis >= 0 && System.currentTimeMillis() - loadedAt > cacheMillis) {
            cache.clear();
            formats.clear();
            loadedAt = System.currentTimeMillis();
        }
        if (cacheMillis == 0) {
            return load(key);
        }
        return cache.computeIfAbsent(key, this::load);
    }

    private Properties load(Locale locale) {
        Properties result = new Properties();
        List<String> suffixes = new ArrayList<>();
        suffixes.add("");
        if (!locale.getLanguage().isEmpty()) {
            suffixes.add("_" + locale.getLanguage());
            if (!locale.getCountry().isEmpty()) {
                suffixes.add("_" + locale.getLanguage() + "_" + locale.getCountry());
            }
        }
        // Earlier base names win, as in Spring's message sources: read them last.
        for (int b = basenames.size() - 1; b >= 0; b--) {
            for (String suffix : suffixes) {
                for (String extension : new String[] {".yaml", ".yml"}) {
                    for (Resource resource : resources(basenames.get(b) + suffix + extension)) {
                        result.putAll(read(resource));
                    }
                }
            }
        }
        return result;
    }

    private List<Resource> resources(String path) {
        try {
            List<Resource> found = new ArrayList<>();
            for (Resource resource : resolver.getResources("classpath*:" + path)) {
                if (resource.exists()) {
                    found.add(resource);
                }
            }
            return found;
        } catch (java.io.IOException e) {
            return Collections.emptyList();
        }
    }

    /** A YAML file as dotted keys; lists become {@code key[0]}, as in Spring's YAML properties. */
    static Properties read(Resource resource) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(resource);
        Properties properties = yaml.getObject();
        Properties strings = new Properties();
        if (properties != null) {
            for (String name : properties.stringPropertyNames()) {
                strings.setProperty(name, properties.getProperty(name));
            }
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (entry.getValue() != null && !strings.containsKey(entry.getKey())) {
                    strings.setProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
        return strings;
    }
}
