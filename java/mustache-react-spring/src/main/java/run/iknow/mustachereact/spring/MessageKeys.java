package run.iknow.mustachereact.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * The message keys defined in the application's message bundles ({@code messages.properties},
 * {@code messages_en.properties}, ...), so that patterns such as {@code orders.*} can be expanded.
 * A {@link org.springframework.context.MessageSource} cannot list its keys, so the bundles are read once from the
 * classpath, with the same base names as Spring Boot's {@code spring.messages.basename}.
 */
public class MessageKeys {

    private static final Log LOGGER = LogFactory.getLog(MessageKeys.class);

    private final ResourcePatternResolver resolver;
    private final List<String> basenames;
    private final Charset encoding;
    private volatile Set<String> keys;
    private volatile boolean cache = true;

    /**
     * @param resolver finds the bundles
     * @param basenames bundle base names, e.g. {@code messages} or {@code i18n/messages}; a {@code classpath:}
     *     prefix is allowed
     * @param encoding encoding of the bundle files
     */
    public MessageKeys(ResourcePatternResolver resolver, Collection<String> basenames, Charset encoding) {
        this.resolver = resolver == null ? new PathMatchingResourcePatternResolver() : resolver;
        this.basenames = new ArrayList<>(basenames);
        this.encoding = encoding;
    }

    /**
     * @param cache whether the bundles are read once; {@code false} reads them on every call, so that keys added in
     *     development show up without a restart
     * @return this
     */
    public MessageKeys cache(boolean cache) {
        this.cache = cache;
        return this;
    }

    /**
     * @return all keys of all bundles and locales, sorted
     */
    public Set<String> all() {
        Set<String> result = cache ? keys : null;
        if (result == null) {
            result = Collections.unmodifiableSet(load());
            keys = result;
        }
        return result;
    }

    private Set<String> load() {
        Set<String> result = new TreeSet<>();
        for (String basename : basenames) {
            String name = basename.trim();
            if (name.isEmpty()) {
                continue;
            }
            for (String prefix : new String[] {"classpath*:", "classpath:"}) {
                if (name.startsWith(prefix)) {
                    name = name.substring(prefix.length());
                }
            }
            name = name.replace('.', '/').replaceAll("^/+", "");
            // messages.properties, messages_en.properties, messages_en_US.properties; not messages-old.properties
            String file = name.substring(name.lastIndexOf('/') + 1);
            Pattern bundleFile = Pattern.compile(Pattern.quote(file) + "(_[A-Za-z0-9_#-]+)?\\.properties");
            try {
                for (Resource resource : resolver.getResources("classpath*:" + name + "*.properties")) {
                    String filename = resource.getFilename();
                    if (filename != null && bundleFile.matcher(filename).matches()) {
                        result.addAll(keysOf(resource));
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("mustache-react: could not list the message bundles of '" + basename + "'", e);
            }
        }
        return result;
    }

    private Set<String> keysOf(Resource resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = resource.getInputStream()) {
            properties.load(new InputStreamReader(in, encoding));
        }
        return properties.stringPropertyNames();
    }
}
