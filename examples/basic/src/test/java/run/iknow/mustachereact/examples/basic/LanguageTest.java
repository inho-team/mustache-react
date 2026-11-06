package run.iknow.mustachereact.examples.basic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/** Korean by default; ?lang=en switches to English and a cookie keeps the choice. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mustache-react.manifest-location=classpath:/test-manifest.json", "mustache-react.ssr=off"})
class LanguageTest {

    @Autowired
    private Environment environment;

    private HttpGet get(String path, String cookie) throws Exception {
        return HttpGet.of("http://localhost:" + environment.getProperty("local.server.port") + path, "text/html", cookie);
    }

    @Test
    void pagesAreKoreanByDefault() throws Exception {
        String html = get("/orders", null).body;

        assertThat(html).contains("<html lang=\"ko\">").contains("<title>주문 목록</title>")
                .contains("\"orders.title\":\"주문 목록\"")
                .contains("\"i18n\":{\"param\":\"lang\",\"locales\":[\"ko\",\"en\"]}")
                // Only the listed keys and prefixes are written into the page.
                .doesNotContain("dashboard.intro").doesNotContain("required.customer");
    }

    @Test
    void theLanguageParameterSwitchesAndTheCookieKeepsIt() throws Exception {
        HttpGet english = get("/orders?lang=en", null);
        assertThat(english.body).contains("<html lang=\"en\">").contains("<title>Orders</title>")
                .contains("\"orders.status.SHIPPED\":\"Shipped\"");
        assertThat(english.setCookie).containsPattern("(?m)^locale=en;");

        String cookie = "locale=en";
        assertThat(get("/orders/1001", cookie).body).contains("<title>Order</title>");
        assertThat(get("/dashboard", cookie).body).contains("<html lang=\"en\">").contains("<h1>Dashboard</h1>")
                .contains("<th>Customer</th>");
        HttpGet korean = get("/dashboard", null);
        // Mustache views get an explicit charset; without it, Korean responses were ISO-8859-1.
        assertThat(korean.contentType).isEqualTo("text/html;charset=UTF-8");
        assertThat(korean.body).contains("<h1>대시보드</h1>").contains("<th>고객</th>");
    }
}
