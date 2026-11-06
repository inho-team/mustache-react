package run.iknow.mustachereact.examples.basic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * The same URL answers with a React page for browsers and with JSON for API clients.
 * Uses a fixed test manifest, so it runs without building the frontend.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mustache-react.manifest-location=classpath:/test-manifest.json", "mustache-react.ssr=off"})
class OrderPageTest {

    private static final String BROWSER_ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8";

    @Autowired
    private Environment environment;

    private HttpGet get(String accept) throws Exception {
        return HttpGet.of("http://localhost:" + environment.getProperty("local.server.port") + "/orders", accept);
    }

    @Test
    void browsersGetTheReactPage() throws Exception {
        HttpGet response = get(BROWSER_ACCEPT);

        assertThat(response.status).isEqualTo(200);
        assertThat(response.contentType).startsWith("text/html").containsIgnoringCase("charset=UTF-8");
        assertThat(response.vary).contains("Accept");
        assertThat(response.body)
                .contains("<title>주문 목록</title>")
                .contains("<link rel=\"stylesheet\" href=\"/mustache-react/assets/mustache-react-test.css\">")
                .contains("<link rel=\"modulepreload\" href=\"/mustache-react/assets/OrderList-test.js\">")
                .contains("<div data-mr-island=\"pages/orders/OrderList\" data-mr-id=\"mr-page\"></div>")
                .contains("<script type=\"module\" src=\"/mustache-react/assets/mustache-react-test.js\"></script>")
                // Props are the Jackson JSON of the DTO, with dates as ISO strings.
                .contains("\"customer\":\"김민수\"")
                .contains("\"orderedAt\":\"2026-11-01\"")
                .contains("\"total\":4");
    }

    @Test
    void propsCannotBreakOutOfTheScriptElement() throws Exception {
        String body = get(BROWSER_ACCEPT).body;

        assertThat(body).doesNotContain("</script><script>alert(1)");
        assertThat(body).contains("\\u003c/script\\u003e\\u003cscript\\u003ealert(1)");
    }

    @Test
    void apiClientsGetJson() throws Exception {
        for (String accept : new String[] {"application/json", "*/*", null}) {
            HttpGet response = get(accept);

            assertThat(response.status).as(accept).isEqualTo(200);
            assertThat(response.contentType).as(accept).startsWith("application/json");
            assertThat(response.vary).as(accept).contains("Accept");
            assertThat(response.body).as(accept)
                    .startsWith("{")
                    .contains("\"customer\":\"김민수\"")
                    .doesNotContain("<html");
        }
    }
}
