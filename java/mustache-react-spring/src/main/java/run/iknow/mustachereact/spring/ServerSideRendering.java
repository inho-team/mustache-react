package run.iknow.mustachereact.spring;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import run.iknow.mustachereact.core.SsrEngine;
import run.iknow.mustachereact.core.SsrException;

/**
 * Decides whether a component is rendered on the server ({@code mustache-react.ssr}).
 *
 * <ul>
 *   <li>{@code off}: never; the browser renders.</li>
 *   <li>{@code auto}: when the engine works; otherwise the page falls back to rendering in the browser, and the
 *       failure is logged once until rendering works again.</li>
 *   <li>{@code always}: a failure fails the request.</li>
 * </ul>
 */
public class ServerSideRendering {

    private static final Log LOGGER = LogFactory.getLog(ServerSideRendering.class);

    /** Rendering modes. */
    public enum Mode {
        /** Browser only. */
        OFF,
        /** Server when possible, browser otherwise. */
        AUTO,
        /** Server, or fail. */
        ALWAYS
    }

    private final Mode mode;
    private final SsrEngine engine;
    private final AtomicBoolean failing = new AtomicBoolean();

    /**
     * @param mode the mode
     * @param engine the engine, may be {@code null} when the mode is {@link Mode#OFF}
     */
    public ServerSideRendering(Mode mode, SsrEngine engine) {
        this.mode = engine == null ? Mode.OFF : mode;
        this.engine = engine;
    }

    /** @return rendering in the browser only */
    public static ServerSideRendering off() {
        return new ServerSideRendering(Mode.OFF, null);
    }

    /** @return the mode */
    public Mode getMode() {
        return mode;
    }

    /**
     * @param component component path, e.g. {@code pages/orders/OrderList}
     * @param propsJson props as JSON
     * @param contextJson the server context as JSON, computed only when rendering on the server
     * @return the server-rendered HTML, or {@code null} to render in the browser
     * @throws IllegalStateException in mode {@code always}, when rendering fails
     */
    public String render(String component, String propsJson, Supplier<String> contextJson) {
        if (mode == Mode.OFF) {
            return null;
        }
        try {
            String html = engine.render(component, propsJson, contextJson.get());
            if (failing.compareAndSet(true, false)) {
                LOGGER.info("mustache-react: server-side rendering works again");
            }
            return html;
        } catch (SsrException ex) {
            if (mode == Mode.ALWAYS) {
                throw new IllegalStateException("mustache-react: server-side rendering failed: " + ex.getMessage(), ex);
            }
            if (failing.compareAndSet(false, true)) {
                LOGGER.warn("mustache-react: server-side rendering failed, rendering in the browser instead "
                        + "(logged once until it works again): " + ex.getMessage());
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("mustache-react: server-side rendering failed: " + ex.getMessage());
            }
            return null;
        }
    }
}
