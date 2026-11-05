package run.iknow.mustachereact.examples.basic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/** A Mustache view with React islands. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "mustache-react.manifest-location=classpath:/test-manifest.json")
class DashboardPageTest {

    @Autowired
    private Environment environment;

    private String dashboard() throws Exception {
        HttpGet response = HttpGet.of("http://localhost:" + environment.getProperty("local.server.port") + "/dashboard",
                "text/html");
        assertThat(response.status).isEqualTo(200);
        return response.body;
    }

    @Test
    void rendersIslandsWithPropsFromTheModelAndTheLoopItem() throws Exception {
        String html = dashboard();

        assertThat(html)
                .contains("<h1>대시보드</h1>")
                .contains("<div data-mr-island=\"islands/OrderChart\" data-mr-id=\"mr-0\"></div>")
                .contains("\"currency\":\"원\"")
                .contains("\"orderedAt\":\"2026-11-01\"")
                // Each loop iteration gets its own island with that order's values.
                .contains("data-mr-props=\"mr-1\">{\"status\":\"PAID\",\"orderId\":1001}")
                .contains("data-mr-props=\"mr-2\">{\"status\":\"SHIPPED\",\"orderId\":1002}")
                .contains("data-mr-props=\"mr-4\">{\"status\":\"PAID\",\"orderId\":1004}");
        assertThat(count(html, "data-mr-island=\"islands/StatusBadge\"")).isEqualTo(4);
    }

    @Test
    void headHasEntryAssetsAndScriptsPreloadTheIslands() throws Exception {
        String html = dashboard();
        String head = html.substring(0, html.indexOf("</head>"));
        String tail = html.substring(html.indexOf("</table>"));

        assertThat(head).contains("<link rel=\"stylesheet\" href=\"/mustache-react/assets/mustache-react-test.css\">")
                .doesNotContain("OrderChart-test.js");
        assertThat(tail)
                .contains("<link rel=\"modulepreload\" href=\"/mustache-react/assets/OrderChart-test.js\">")
                .contains("<link rel=\"modulepreload\" href=\"/mustache-react/assets/StatusBadge-test.js\">")
                .contains("<script type=\"module\" src=\"/mustache-react/assets/mustache-react-test.js\"></script>");
        assertThat(count(tail, "StatusBadge-test.js")).isEqualTo(1);
    }

    @Test
    void escapesMarkupInIslandProps() throws Exception {
        String html = dashboard();

        // Mustache escapes the table cell; the island props are script-safe JSON.
        assertThat(html).doesNotContain("<script>alert(1)");
        assertThat(html).contains("\\u003c/script\\u003e\\u003cscript\\u003ealert(1)");
    }

    private static int count(String text, String needle) {
        Matcher matcher = Pattern.compile(Pattern.quote(needle)).matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
