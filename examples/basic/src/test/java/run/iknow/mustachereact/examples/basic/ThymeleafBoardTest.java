package run.iknow.mustachereact.examples.basic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/** A Thymeleaf view with <react:...> islands, next to the Mustache views. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mustache-react.manifest-location=classpath:/test-manifest.json", "mustache-react.ssr=off"})
class ThymeleafBoardTest {

    @Autowired
    private Environment environment;

    private HttpGet get(String path) throws Exception {
        return HttpGet.of("http://localhost:" + environment.getProperty("local.server.port") + path, "text/html");
    }

    @Test
    void rendersIslandsWithObjectPropsInsideThymeleafLoops() throws Exception {
        HttpGet board = get("/board");

        assertThat(board.status).isEqualTo(200);
        assertThat(board.contentType).isEqualTo("text/html;charset=UTF-8");
        assertThat(board.body)
                .contains("<html lang=\"ko\">").contains("<h1>주문 보드</h1>").contains("<th>고객</th>")
                .contains("<link rel=\"stylesheet\" href=\"/mustache-react/assets/mustache-react-test.css\">")
                // ${orders} is a list of objects, ${currency} a string
                .contains("<div data-mr-island=\"islands/OrderChart\" data-mr-id=\"mr-0\"></div>")
                .contains("\"currency\":\"원\"").contains("\"orderedAt\":\"2026-11-01\"")
                // one island per th:each row, order-id becomes orderId
                .contains("data-mr-props=\"mr-1\">{\"status\":\"PAID\",\"orderId\":1001}")
                .contains("data-mr-props=\"mr-2\">{\"status\":\"SHIPPED\",\"orderId\":1002}")
                .contains("<link rel=\"modulepreload\" href=\"/mustache-react/assets/StatusBadge-test.js\">")
                .contains("<script type=\"application/json\" id=\"mr-context\">")
                .contains("<script type=\"module\" src=\"/mustache-react/assets/mustache-react-test.js\"></script>")
                .doesNotContain("<react:");
    }

    @Test
    void followsTheLanguage() throws Exception {
        assertThat(get("/board?lang=en").body).contains("<html lang=\"en\">").contains("<h1>Order board</h1>")
                .contains("<th>Customer</th>").contains("\"orders.status.PAID\":\"Paid\"");
    }

    @Test
    void escapesMarkupInProps() throws Exception {
        assertThat(get("/board").body).doesNotContain("</script><script>alert(1)")
                .contains("\\u003c/script\\u003e\\u003cscript\\u003ealert(1)");
    }
}
