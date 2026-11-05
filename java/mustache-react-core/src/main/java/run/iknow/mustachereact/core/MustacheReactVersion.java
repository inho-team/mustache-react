package run.iknow.mustachereact.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Version of the mustache-react Java artifacts.
 *
 * <p>The JS runtime ({@code @mustache-react/client}) is released with the same version. It is used
 * to detect a mismatch between the server library and the frontend bundle.
 */
public final class MustacheReactVersion {

    private static final String RESOURCE = "/META-INF/mustache-react/version.properties";

    private static final String VERSION = load();

    private MustacheReactVersion() {
    }

    /**
     * Returns the library version, for example {@code 0.1.0}, or {@code "unknown"} if it cannot be read.
     *
     * @return the library version
     */
    public static String get() {
        return VERSION;
    }

    private static String load() {
        try (InputStream in = MustacheReactVersion.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return "unknown";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version", "unknown");
        } catch (IOException ex) {
            return "unknown";
        }
    }
}
