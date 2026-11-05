package run.iknow.mustachereact.autoconfigure;

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

    /** Directory holding the React sources, relative to the project root. Used in development mode. */
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
