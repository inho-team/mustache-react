package run.iknow.mustachereact.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code mustache-react.*} in {@code application.yml}.
 */
@ConfigurationProperties(prefix = MustacheReactProperties.PREFIX)
public class MustacheReactProperties {

    /** Property prefix. */
    public static final String PREFIX = "mustache-react";

    /** Whether mustache-react is enabled. */
    private boolean enabled = true;

    /** Server-side rendering mode. */
    private SsrMode ssr = SsrMode.OFF;

    /**
     * Directory holding the React sources, relative to the working directory. Used in development mode; the Gradle
     * plugin passes the absolute path to {@code bootRun} and {@code run}.
     */
    private String frontendDir = "src/main/frontend";

    /**
     * Whether a {@code @ReactPage} URL answers with JSON instead of HTML when the request asks for
     * {@code application/json}.
     */
    private boolean contentNegotiation = true;

    /** Location of the Vite manifest written by the frontend build. */
    private String manifestLocation = "classpath:/static/mustache-react/.vite/manifest.json";

    /**
     * Where pages load the built frontend assets from. A path ({@code /mustache-react/}, the default) serves them
     * from the application jar; a URL ({@code https://cdn.example.com/shop/}) loads them from a CDN or another
     * server where the same files were uploaded. The build is the same either way.
     */
    private String publicPath = "/mustache-react/";

    /** Mustache layout for {@code @ReactPage} pages. The built-in layout is used when this resource does not exist. */
    private String layout = "classpath:/templates/mustache-react/layout.mustache";

    /** Names under which Mustache views see the mustache-react helpers. */
    private final Template template = new Template();

    /** Loading the frontend from a running Vite dev server, for development. */
    private final DevServer devServer = new DevServer();

    /** What the server context ({@code useServerContext()}) exposes. */
    private final Context context = new Context();

    /** The Node.js server that renders pages when {@code mustache-react.ssr} is {@code auto} or {@code always}. */
    private final SsrServer ssrServer = new SsrServer();

    /** Language switching and the languages on offer. */
    private final I18n i18n = new I18n();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SsrMode getSsr() {
        return ssr;
    }

    public void setSsr(SsrMode ssr) {
        this.ssr = ssr;
    }

    public String getFrontendDir() {
        return frontendDir;
    }

    public void setFrontendDir(String frontendDir) {
        this.frontendDir = frontendDir;
    }

    public boolean isContentNegotiation() {
        return contentNegotiation;
    }

    public void setContentNegotiation(boolean contentNegotiation) {
        this.contentNegotiation = contentNegotiation;
    }

    public String getManifestLocation() {
        return manifestLocation;
    }

    public void setManifestLocation(String manifestLocation) {
        this.manifestLocation = manifestLocation;
    }

    public String getPublicPath() {
        return publicPath;
    }

    public void setPublicPath(String publicPath) {
        this.publicPath = publicPath;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public Template getTemplate() {
        return template;
    }

    public DevServer getDevServer() {
        return devServer;
    }

    public Context getContext() {
        return context;
    }

    public I18n getI18n() {
        return i18n;
    }

    public SsrServer getSsrServer() {
        return ssrServer;
    }

    /**
     * Settings under {@code mustache-react.ssr-server.*}. The Node.js executable is the one of
     * {@code mustache-react.dev-server.node} / {@code node-install-dir}.
     */
    public static class SsrServer {

        /** The SSR bundle built with {@code ssr = true} in the Gradle or Maven plugin. */
        private String bundleLocation = "classpath:/META-INF/mustache-react/ssr/server.mjs";

        /** How long one render may take before the page falls back to rendering in the browser. */
        private Duration timeout = Duration.ofSeconds(3);

        /** How long to wait for the SSR server to start. */
        private Duration startupTimeout = Duration.ofSeconds(15);

        public String getBundleLocation() {
            return bundleLocation;
        }

        public void setBundleLocation(String bundleLocation) {
            this.bundleLocation = bundleLocation;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Duration getStartupTimeout() {
            return startupTimeout;
        }

        public void setStartupTimeout(Duration startupTimeout) {
            this.startupTimeout = startupTimeout;
        }
    }

    /** Settings under {@code mustache-react.i18n.*}. */
    public static class I18n {

        /**
         * Request parameter that switches the language, e.g. {@code lang} for {@code ?lang=en}. The choice is kept
         * in a cookie. Unset: the language follows the {@code Accept-Language} header (Spring Boot's default).
         */
        private String param;

        /** Languages the application offers, e.g. {@code ko,en}; written into the page for a language menu. */
        private List<Locale> locales = new ArrayList<>();

        /**
         * Language before the user picks one. Unset: the {@code Accept-Language} header. Only used with
         * {@code param}.
         */
        private Locale defaultLocale;

        /** Cookie that keeps the chosen language. */
        private String cookieName = "locale";

        /** How long the cookie keeps the chosen language. */
        private Duration cookieMaxAge = Duration.ofDays(365);

        public String getParam() {
            return param;
        }

        public void setParam(String param) {
            this.param = param;
        }

        public List<Locale> getLocales() {
            return locales;
        }

        public void setLocales(List<Locale> locales) {
            this.locales = locales;
        }

        public Locale getDefaultLocale() {
            return defaultLocale;
        }

        public void setDefaultLocale(Locale defaultLocale) {
            this.defaultLocale = defaultLocale;
        }

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

        public Duration getCookieMaxAge() {
            return cookieMaxAge;
        }

        public void setCookieMaxAge(Duration cookieMaxAge) {
            this.cookieMaxAge = cookieMaxAge;
        }
    }

    /** Settings under {@code mustache-react.context.*}. */
    public static class Context {

        /** Whether to expose the CSRF token (with Spring Security). */
        private boolean csrf = true;

        /** Whether to expose the signed-in user's name and authorities (with Spring Security). */
        private boolean user = true;

        /**
         * Message keys to expose in the request locale: keys ({@code app.name}), prefixes ({@code orders.*}) or
         * {@code *} for all. Only these are written into the page.
         */
        private List<String> messages = new ArrayList<>();

        public boolean isCsrf() {
            return csrf;
        }

        public void setCsrf(boolean csrf) {
            this.csrf = csrf;
        }

        public boolean isUser() {
            return user;
        }

        public void setUser(boolean user) {
            this.user = user;
        }

        public List<String> getMessages() {
            return messages;
        }

        public void setMessages(List<String> messages) {
            this.messages = messages;
        }
    }

    /** Settings under {@code mustache-react.dev-server.*}. */
    public static class DevServer {

        /**
         * Whether pages load the frontend from the Vite dev server (hot module replacement, no build needed)
         * instead of the built files. Enable it in a development profile only.
         */
        private boolean enabled = false;

        /** Origin of the Vite dev server. The public path is appended. */
        private String url = "http://localhost:5173";

        /**
         * Whether to start the dev server with the application (and stop it with the application). A dev server
         * already listening on the port is used instead.
         */
        private boolean autoStart = true;

        /** Node.js executable for the dev server. Default: the newest one in {@code node-install-dir}, else {@code node}. */
        private String node = "";

        /** Where the Gradle and Maven plugins download Node.js. */
        private String nodeInstallDir = ".mustache-react/node";

        /** How long to wait for the dev server to start. */
        private Duration startupTimeout = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isAutoStart() {
            return autoStart;
        }

        public void setAutoStart(boolean autoStart) {
            this.autoStart = autoStart;
        }

        public String getNode() {
            return node;
        }

        public void setNode(String node) {
            this.node = node;
        }

        public String getNodeInstallDir() {
            return nodeInstallDir;
        }

        public void setNodeInstallDir(String nodeInstallDir) {
            this.nodeInstallDir = nodeInstallDir;
        }

        public Duration getStartupTimeout() {
            return startupTimeout;
        }

        public void setStartupTimeout(Duration startupTimeout) {
            this.startupTimeout = startupTimeout;
        }
    }

    /** Settings under {@code mustache-react.template.*}. */
    public static class Template {

        /** Whether to add the island section and the helpers to every Spring MVC model. */
        private boolean modelAttributes = true;

        /** Model name of the island section: {@code {{#react}}OrderChart orders{{/react}}}. */
        private String reactName = "react";

        /** Model name of the helpers: {@code {{{mr.head}}}}, {@code {{{mr.scripts}}}}. */
        private String helpersName = "mr";

        /**
         * Whether Mustache templates may write islands as HTML tags:
         * {@code <react:StatusBadge status="{{status}}" />}, {@code <react:head />}, {@code <react:scripts />}.
         * The tag prefix is the island section name ({@code react-name}).
         */
        private boolean htmlTags = true;

        public boolean isHtmlTags() {
            return htmlTags;
        }

        public void setHtmlTags(boolean htmlTags) {
            this.htmlTags = htmlTags;
        }

        public boolean isModelAttributes() {
            return modelAttributes;
        }

        public void setModelAttributes(boolean modelAttributes) {
            this.modelAttributes = modelAttributes;
        }

        public String getReactName() {
            return reactName;
        }

        public void setReactName(String reactName) {
            this.reactName = reactName;
        }

        public String getHelpersName() {
            return helpersName;
        }

        public void setHelpersName(String helpersName) {
            this.helpersName = helpersName;
        }
    }

    /** Server-side rendering mode. */
    public enum SsrMode {
        /** Render in the browser only. */
        OFF,
        /** Render on the server when it works, otherwise in the browser (failures are logged). */
        AUTO,
        /** Always render on the server. Fails at startup without the SSR bundle, and fails requests that cannot be rendered. */
        ALWAYS
    }
}
