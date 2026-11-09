package run.iknow.mustachereact.spring.thymeleaf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import run.iknow.mustachereact.spring.IslandRenderer;
import run.iknow.mustachereact.spring.ManifestFrontendAssets;
import run.iknow.mustachereact.spring.PropsSerializer;
import run.iknow.mustachereact.spring.ServerSideRendering;
import run.iknow.mustachereact.spring.ViteManifestSource;
import run.iknow.mustachereact.spring.ThymeleafTestSupport;

class MustacheReactDialectTest {

    private TemplateEngine engine;

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(ThymeleafTestSupport.requestAttributes());
        PropsSerializer serializer = new PropsSerializer(
                Collections.<HttpMessageConverter<?>>singletonList(ThymeleafTestSupport.jsonConverter()));
        ViteManifestSource manifest =
                new ViteManifestSource(new DefaultResourceLoader(), "classpath:/renderer/manifest.json");
        IslandRenderer islands = new IslandRenderer(serializer, ServerSideRendering.off(), null,
                new ManifestFrontendAssets(manifest, "/mustache-react/"));
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.addDialect(new MustacheReactDialect(islands));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private String render(String template, Map<String, Object> variables) {
        return engine.process(template, new Context(Locale.KOREAN, variables));
    }

    @Test
    void rendersAnIslandWithExpressionAndLiteralProps() {
        String html = render("<p><react:OrderChart total=\"${total}\" currency=\"원\" order-id=\"${7}\" open /></p>",
                Collections.<String, Object>singletonMap("total", 3));

        assertThat(html).isEqualTo("<p><div data-mr-island=\"islands/OrderChart\" data-mr-id=\"mr-0\"></div>\n"
                + "<script type=\"application/json\" data-mr-props=\"mr-0\">"
                + "{\"total\":3,\"currency\":\"원\",\"orderId\":7,\"open\":true}</script></p>");
    }

    @Test
    void repeatsWithThEachAndSkipsWithThIf() {
        String template = "<ul><li th:each=\"s : ${statuses}\"><react:StatusBadge status=\"${s}\" /></li></ul>"
                + "<react:Hidden th:if=\"${false}\" /><react:order-total th:each=\"n : ${statuses}\" th:if=\"${n == 'PAID'}\" n=\"${n}\"/>";
        String html = render(template, Collections.<String, Object>singletonMap("statuses", Arrays.asList("PAID", "SHIPPED")));

        assertThat(html).contains("data-mr-props=\"mr-0\">{\"status\":\"PAID\"}")
                .contains("data-mr-props=\"mr-1\">{\"status\":\"SHIPPED\"}")
                .doesNotContain("Hidden")
                // kebab-case names map to the component file name
                .contains("data-mr-island=\"islands/OrderTotal\" data-mr-id=\"mr-2\"")
                .contains("{\"n\":\"PAID\"}").doesNotContain("mr-3");
    }

    @Test
    void namesComponentsInSubdirectoriesAndWritesTheAssets() {
        String html = render("<html><head><react:head/></head><body>"
                + "<react:island component=\"charts/OrderChart\" title=\"x\"/><react:scripts/></body></html>",
                Collections.<String, Object>emptyMap());

        assertThat(html).contains("data-mr-island=\"islands/charts/OrderChart\"").contains("{\"title\":\"x\"}")
                .contains("<script type=\"application/json\" id=\"mr-context\">{}</script>")
                .contains("<script type=\"module\"").doesNotContain("react:");
    }

    @Test
    void reportsBadTagsWithTheElementName() {
        assertThatThrownBy(() -> render("<react:island title=\"x\"/>", Collections.<String, Object>emptyMap()))
                .hasStackTraceContaining("<react:island> needs component=");
        assertThatThrownBy(() -> render("<react:Bad..Name/>", Collections.<String, Object>emptyMap()))
                .hasStackTraceContaining("Invalid component name");
    }

    @Test
    void convertsAttributeNames() {
        assertThat(IslandElementProcessor.propName("order-id")).isEqualTo("orderId");
        assertThat(IslandElementProcessor.propName("orderId")).isEqualTo("orderId");
        assertThat(IslandElementProcessor.componentName("status-badge")).isEqualTo("StatusBadge");
        assertThat(IslandElementProcessor.componentName("StatusBadge")).isEqualTo("StatusBadge");
    }
}
