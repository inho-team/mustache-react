package run.iknow.mustachereact.examples.basic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Sign-in with Spring Security, the server context, and the order form: validation errors, redirect, CSRF.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"mustache-react.manifest-location=classpath:/test-manifest.json", "mustache-react.ssr=off"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // adds orders
class OrderFormTest {

    @Autowired
    private Environment environment;

    private Browser browser;

    @BeforeEach
    void signIn() throws Exception {
        browser = new Browser("http://localhost:" + environment.getProperty("local.server.port"));

        Browser.Response protectedPage = browser.get("/orders/new", "text/html");
        assertThat(protectedPage.status).isEqualTo(302);
        assertThat(protectedPage.header("Location")).endsWith("/login");

        Browser.Response login = browser.get("/login", "text/html");
        assertThat(login.body).contains("data-mr-island=\"pages/auth/Login\"").contains("\"user\":null");
        Map<String, String> form = new LinkedHashMap<>();
        form.put("username", "kim");
        form.put("password", "password");
        form.put("_csrf", login.csrfToken());
        Browser.Response signedIn = browser.postForm("/login", form);
        assertThat(signedIn.status).isEqualTo(302);
        assertThat(signedIn.header("Location")).doesNotContain("error");
    }

    private String formPageToken() throws Exception {
        Browser.Response page = browser.get("/orders/new", "text/html");
        assertThat(page.status).isEqualTo(200);
        // Spring Security 7 adds factor authorities (FACTOR_PASSWORD); Jackson 3 orders properties alphabetically.
        assertThat(page.body)
                .containsPattern("\"user\":\\{\"name\":\"kim\",\"authorities\":\\[[^\\]]*\"ROLE_USER\"")
                .contains("\"messages\":{\"app.name\":\"mustache-react 예제\",")
                .contains("\"locale\":")
                .contains("\"customer\":\"\"").contains("\"amount\":null");
        return page.csrfToken();
    }

    @Test
    void validationErrorsComeBackAsJsonWithLocalizedMessages() throws Exception {
        Browser.Response response = browser.postJson("/orders/new", "application/json",
                "{\"customer\":\" \",\"amount\":0}", formPageToken());

        assertThat(response.status).isEqualTo(400);
        assertThat(response.body).isEqualTo("{\"errors\":{\"fields\":{"
                + "\"customer\":[\"고객 이름을 입력하세요.\"],\"amount\":[\"금액은 1원 이상이어야 합니다.\"]},\"global\":[]}}");
    }

    @Test
    void aValidOrderIsSavedAndRedirects() throws Exception {
        Browser.Response response = browser.postJson("/orders/new", "application/json",
                "{\"customer\":\"최유나\",\"amount\":45000}", formPageToken());

        assertThat(response.status).isEqualTo(200);
        assertThat(response.body).isEqualTo("{\"redirect\":\"/orders\"}");
        assertThat(browser.get("/orders", "application/json").body).contains("\"customer\":\"최유나\"").contains("\"amount\":45000");
    }

    @Test
    void browsersGetA303OrThePageWithErrorsInTheServerContext() throws Exception {
        String token = formPageToken();

        Browser.Response redirect = browser.postJson("/orders/new", "text/html", "{\"customer\":\"정하늘\",\"amount\":100}", token);
        assertThat(redirect.status).isEqualTo(303);
        assertThat(redirect.header("Location")).isEqualTo("/orders");

        Browser.Response invalid = browser.postJson("/orders/new", "text/html", "{\"customer\":\"\",\"amount\":5}", token);
        assertThat(invalid.status).isEqualTo(200);
        assertThat(invalid.body)
                .contains("data-mr-island=\"pages/orders/OrderForm\"")
                .contains("\"errors\":{\"fields\":{\"customer\":[\"고객 이름을 입력하세요.\"]},\"global\":[]}");
    }

    @Test
    void rejectsPostsWithoutTheCsrfToken() throws Exception {
        Browser.Response response = browser.postJson("/orders/new", "application/json",
                "{\"customer\":\"x\",\"amount\":1}", null);

        assertThat(response.status).isEqualTo(403);
    }

    private static java.util.Map<String, String> fields(String customer, String amount, String token) {
        java.util.Map<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("customer", customer);
        fields.put("amount", amount);
        fields.put("_csrf", token);
        return fields;
    }

    @Test
    void formPostsBindWithModelAttributeToTheSameUrl() throws Exception {
        String token = formPageToken();

        // useSpringForm({encoding: 'form'}): JSON answers
        Browser.Response errors = browser.postForm("/orders/new", "application/json", fields(" ", "", token));
        assertThat(errors.status).isEqualTo(400);
        assertThat(errors.body).contains("\"customer\":[\"고객 이름을 입력하세요.\"]");

        // A plain HTML form without JavaScript: the page again with the errors, or a 303
        Browser.Response page = browser.postForm("/orders/new", "text/html", fields("", "5", token));
        assertThat(page.status).isEqualTo(200);
        assertThat(page.body).contains("\"errors\":{\"fields\":{\"customer\":[\"고객 이름을 입력하세요.\"]}");

        Browser.Response saved = browser.postForm("/orders/new", "text/html", fields("한지우", "12000", token));
        assertThat(saved.status).isEqualTo(303);
        assertThat(saved.header("Location")).isEqualTo("/orders");
        assertThat(browser.get("/orders", "application/json").body).contains("\"customer\":\"한지우\"");
    }
}
