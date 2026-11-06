package run.iknow.mustachereact.examples.basic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import run.iknow.mustachereact.core.SsrEngine;
import run.iknow.mustachereact.core.SsrException;

/**
 * mustache-react.ssr=auto with a stand-in engine: pages and islands carry server HTML for hydration, and a failing
 * component falls back to rendering in the browser. The real Node.js rendering is covered by the browser e2e test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "mustache-react.manifest-location=classpath:/test-manifest.json",
        "mustache-react.ssr=auto",
        "mustache-react.ssr-server.bundle-location=classpath:/no-bundle.mjs"})
@Import(ServerSideRenderingTest.FakeEngine.class)
class ServerSideRenderingTest {

    @TestConfiguration
    static class FakeEngine {
        @Bean
        SsrEngine ssrEngine() {
            return (component, props, context) -> {
                if (component.equals("islands/OrderChart")) {
                    throw new SsrException("chart cannot render on the server");
                }
                return "<p>server:" + component + "</p>";
            };
        }
    }

    @Autowired
    private Environment environment;

    private String get(String path) throws Exception {
        HttpGet response = HttpGet.of("http://localhost:" + environment.getProperty("local.server.port") + path, "text/html");
        assertThat(response.status).isEqualTo(200);
        return response.body;
    }

    @Test
    void pagesAreRenderedOnTheServer() throws Exception {
        assertThat(get("/orders")).contains("<div data-mr-island=\"pages/orders/OrderList\" data-mr-id=\"mr-page\" "
                + "data-mr-ssr=\"true\"><p>server:pages/orders/OrderList</p></div>");
    }

    @Test
    void islandsAreRenderedOnTheServerAndFailuresFallBackToTheBrowser() throws Exception {
        String html = get("/dashboard");

        assertThat(html).contains("data-mr-ssr=\"true\"><p>server:islands/StatusBadge</p></div>");
        assertThat(html).contains("<div data-mr-island=\"islands/OrderChart\" data-mr-id=\"mr-0\"></div>");
    }
}
