package run.iknow.mustachereact.examples.basic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/** A @ReactPage method returning ResponseEntity: its status and headers apply to the page and to the JSON. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mustache-react.manifest-location=classpath:/test-manifest.json", "mustache-react.ssr=off"})
class OrderDetailTest {

    @Autowired
    private Environment environment;

    private Browser.Response get(String path, String accept) throws Exception {
        return new Browser("http://localhost:" + environment.getProperty("local.server.port")).get(path, accept);
    }

    @Test
    void foundOrdersUseTheEntityHeaders() throws Exception {
        Browser.Response page = get("/orders/1002", "text/html");
        Browser.Response json = get("/orders/1002", "application/json");

        assertThat(page.status).isEqualTo(200);
        assertThat(page.header("Cache-Control")).isEqualTo("no-store");
        assertThat(page.header("Content-Type")).startsWith("text/html");
        assertThat(page.body).contains("data-mr-island=\"pages/orders/OrderDetail\"").contains("\"customer\":\"이서연\"");
        assertThat(json.status).isEqualTo(200);
        assertThat(json.header("Cache-Control")).isEqualTo("no-store");
        assertThat(json.header("Vary")).contains("Accept");
        assertThat(json.body).contains("\"id\":1002").contains("\"customer\":\"이서연\"");
    }

    @Test
    void missingOrdersAreA404PageAndA404Json() throws Exception {
        Browser.Response page = get("/orders/9999", "text/html");
        Browser.Response json = get("/orders/9999", "application/json");

        assertThat(page.status).isEqualTo(404);
        assertThat(page.body).contains("data-mr-island=\"pages/orders/OrderDetail\"").contains("\"order\":null");
        assertThat(json.status).isEqualTo(404);
        assertThat(json.body).contains("\"id\":9999").contains("\"order\":null");
    }
}
