package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;

import com.samskivert.mustache.Mustache;

class HtmlIslandTagsTest {

    private final Map<String, Object> model = new HashMap<>();
    private final Map<String, String> partials = new HashMap<>();
    private Mustache.Compiler compiler;

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(new MapRequestAttributes());
        PropsSerializer serializer = new PropsSerializer(
                Collections.<HttpMessageConverter<?>>singletonList(new ReactIslandLambdaTest.MapJsonConverter()));
        model.put("react", new ReactIslandLambda(serializer));
        model.put("mr", new MustacheReactHelpers(new ManifestFrontendAssets(new ViteManifestSource(
                new DefaultResourceLoader(), "classpath:/renderer/manifest.json"), "/mustache-react/"),
                ReactPageRendererTest.context()));
        compiler = new IslandTagCompiler(Mustache.compiler().withLoader(name -> new StringReader(partials.get(name))),
                "react", "react", "mr");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private String render(String template) {
        return compiler.compile(template).execute(model);
    }

    @Test
    void passesModelValuesAsObjectsAndOtherValuesAsText() {
        model.put("orders", Arrays.asList(new ReactIslandLambdaTest.Order(1, "PAID")));
        model.put("id", 7);

        String html = render("<react:OrderChart orders=\"{{orders}}\" currency=\"원\" title=\"주문 {{id}}\" open />");

        assertThat(html).isEqualTo("<div data-mr-island=\"islands/OrderChart\" data-mr-id=\"mr-0\"></div>\n"
                + "<script type=\"application/json\" data-mr-props=\"mr-0\">"
                + "{\"orders\":[{\"id\":1,\"status\":\"PAID\"}],\"currency\":\"원\",\"title\":\"주문 7\",\"open\":true}</script>");
    }

    @Test
    void repeatsInsideSectionsWithTheLoopItemFirst() {
        model.put("orders", Arrays.asList(new ReactIslandLambdaTest.Order(1, "PAID"), new ReactIslandLambdaTest.Order(2, "SHIPPED")));

        String html = render("{{#orders}}<td><react:status-badge status=\"{{status}}\" order-id=\"{{id}}\"></react:status-badge></td>{{/orders}}");

        assertThat(html).contains("data-mr-island=\"islands/StatusBadge\" data-mr-id=\"mr-0\"")
                .contains("data-mr-props=\"mr-0\">{\"status\":\"PAID\",\"orderId\":1}")
                .contains("data-mr-props=\"mr-1\">{\"status\":\"SHIPPED\",\"orderId\":2}");
    }

    @Test
    void writesTheAssetsAndNamesSubdirectoriesAndWorksInPartials() {
        partials.put("chart", "<react:island component=\"charts/OrderChart\" label=\"x &amp; y\" />");

        String html = render("<head><react:head/></head><body>{{> chart}}<react:scripts /></body>");

        assertThat(html).contains("data-mr-island=\"islands/charts/OrderChart\"").contains("{\"label\":\"x \\u0026 y\"}")
                .contains("<script type=\"application/json\" id=\"mr-context\">")
                .contains("<script type=\"module\"").doesNotContain("react:");
    }

    @Test
    void keepsTheSectionSyntaxAndOtherTags() {
        model.put("total", 3);

        assertThat(render("{{#react}}OrderChart total{{/react}}<reactive>{{total}}</reactive>"))
                .contains("{\"total\":3}").endsWith("<reactive>3</reactive>");
    }

    @Test
    void reportsMissingModelValues() {
        assertThatThrownBy(() -> render("<react:OrderChart orders=\"{{nothing}}\" />"))
                .hasMessageContaining("'nothing' was not found");
        assertThatThrownBy(() -> render("<react:island title=\"x\" />")).hasMessageContaining("component=");
    }

    @Test
    void rewritesOnlyIslandTags() {
        assertThat(HtmlIslandTags.expand("<p>plain</p>", "react", "react", "mr")).isEqualTo("<p>plain</p>");
        assertThat(HtmlIslandTags.expand("<ui:Chart a=\"{{a}}\"/>", "ui", "react", "mr"))
                .isEqualTo("{{#react}}<ui:Chart a=\"{{a}}\"/>{{/react}}");
    }
}
