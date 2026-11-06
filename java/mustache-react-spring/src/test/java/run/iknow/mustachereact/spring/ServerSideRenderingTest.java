package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import run.iknow.mustachereact.core.SsrEngine;
import run.iknow.mustachereact.core.SsrException;

class ServerSideRenderingTest {

    private final AtomicInteger calls = new AtomicInteger();
    private final SsrEngine working = (component, props, context) -> {
        calls.incrementAndGet();
        return "<p>" + component + " " + props + " " + context + "</p>";
    };
    private final SsrEngine broken = (component, props, context) -> {
        calls.incrementAndGet();
        throw new SsrException("server down");
    };

    @Test
    void offNeverCallsTheEngine() {
        assertThat(new ServerSideRendering(ServerSideRendering.Mode.OFF, working).render("pages/A", "{}", () -> "{}")).isNull();
        assertThat(ServerSideRendering.off().render("pages/A", "{}", () -> {
            throw new AssertionError("context must not be computed");
        })).isNull();
        assertThat(calls).hasValue(0);
    }

    @Test
    void autoRendersOrFallsBackToTheBrowser() {
        assertThat(new ServerSideRendering(ServerSideRendering.Mode.AUTO, working).render("pages/A", "{\"a\":1}", () -> "{\"c\":2}"))
                .isEqualTo("<p>pages/A {\"a\":1} {\"c\":2}</p>");

        ServerSideRendering failing = new ServerSideRendering(ServerSideRendering.Mode.AUTO, broken);
        assertThat(failing.render("pages/A", "{}", () -> "{}")).isNull();
        assertThat(failing.render("pages/A", "{}", () -> "{}")).isNull();
    }

    @Test
    void alwaysFailsTheRequest() {
        ServerSideRendering always = new ServerSideRendering(ServerSideRendering.Mode.ALWAYS, broken);

        assertThatThrownBy(() -> always.render("pages/A", "{}", () -> "{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("server down");
    }
}
