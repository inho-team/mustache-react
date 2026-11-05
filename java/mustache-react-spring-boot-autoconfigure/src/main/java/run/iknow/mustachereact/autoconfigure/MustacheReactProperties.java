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
