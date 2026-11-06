package run.iknow.mustachereact.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

    /** URL path the built frontend assets are served from. Must match the Vite {@code base}. */
    private String publicPath = "/mustache-react/";

    /** Mustache layout for {@code @ReactPage} pages. The built-in layout is used when this resource does not exist. */
    private String layout = "classpath:/templates/mustache-react/layout.mustache";

    /** Names under which Mustache views see the mustache-react helpers. */
    private final Template template = new Template();

    /** Loading the frontend from a running Vite dev server, for development. */
    private final DevServer devServer = new DevServer();

    /** What the server context ({@code useServerContext()}) exposes. */
    private final Context context = new Context();

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

    /** Settings under {@code mustache-react.context.*}. */
    public static class Context {

        /** Whether to expose the CSRF token (with Spring Security). */
        private boolean csrf = true;

        /** Whether to expose the signed-in user's name and authorities (with Spring Security). */
        private boolean user = true;

        /** Message keys to expose in the request locale. Only listed keys are written into the page. */
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
        /** Render on the server when an SSR engine is available, otherwise in the browser. */
        AUTO,
        /** Always render on the server. Fails at startup if no SSR engine is available. */
        ALWAYS
    }
}
