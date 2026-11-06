package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;

import com.samskivert.mustache.Mustache;

class ReactIslandLambdaTest {

    /** Writes Maps of simple values as JSON, standing in for Jackson. */
    static class MapJsonConverter extends PropsSerializerTest.FakeJsonConverter {
        @Override
        public void write(Object o, org.springframework.http.MediaType contentType,
                org.springframework.http.HttpOutputMessage outputMessage) throws java.io.IOException {
            outputMessage.getBody().write(json(o).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        private static String json(Object value) {
            if (value == null) {
                return "null";
            }
            if (value instanceof Map) {
                StringBuilder sb = new StringBuilder("{");
                for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                    if (sb.length() > 1) {
                        sb.append(',');
                    }
                    sb.append('"').append(e.getKey()).append("\":").append(json(e.getValue()));
                }
                return sb.append('}').toString();
            }
            if (value instanceof List) {
                StringBuilder sb = new StringBuilder("[");
                for (Object item : (List<?>) value) {
                    if (sb.length() > 1) {
                        sb.append(',');
                    }
                    sb.append(json(item));
                }
                return sb.append(']').toString();
            }
            if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            if (value instanceof Order) {
                Order order = (Order) value;
                return "{\"id\":" + order.getId() + ",\"status\":\"" + order.getStatus() + "\"}";
            }
            return "\"" + value + "\"";
        }
    }

    public static class Order {
        private final int id;
        private final String status;

        Order(int id, String status) {
            this.id = id;
            this.status = status;
        }

        public int getId() {
            return id;
        }

        public String getStatus() {
            return status;
        }
    }

    /** Accessor style of Java records ({@code total()}), written as a class so it compiles for Java 8. */
    public static final class Summary {
        public int total() {
            return 7;
        }
    }

    private final PropsSerializer serializer = new PropsSerializer(
            Collections.<HttpMessageConverter<?>>singletonList(new MapJsonConverter()));
    private final ViteManifestSource manifest =
            new ViteManifestSource(new DefaultResourceLoader(), "classpath:/renderer/manifest.json");

    private Map<String, Object> model;

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(new MapRequestAttributes());
        model = new HashMap<>();
        model.put("react", new ReactIslandLambda(serializer));
        model.put("mr", new MustacheReactHelpers(new ManifestFrontendAssets(manifest, "/mustache-react/"),
                ReactPageRendererTest.context()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private String render(String template) {
        return Mustache.compiler().compile(template).execute(model);
    }

    @Test
    void passesModelValuesAsProps() {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", "Kim");
        model.put("total", 3);
        model.put("currentUser", user);

        String html = render("{{#react}}OrderChart total name=currentUser.name{{/react}}");

        assertThat(html).isEqualTo("<div data-mr-island=\"islands/OrderChart\" data-mr-id=\"mr-0\"></div>\n"
                + "<script type=\"application/json\" data-mr-props=\"mr-0\">{\"total\":3,\"name\":\"Kim\"}</script>");
    }

    @Test
    void readsRecordStyleAccessors() {
        model.put("summary", new Summary());

        assertThat(render("{{#react}}OrderChart total=summary.total{{/react}}")).contains("{\"total\":7}");
    }

    @Test
    void resolvesFromTheLoopItemFirstThenTheModel() {
        model.put("orders", Arrays.asList(new Order(1, "PAID"), new Order(2, "SHIPPED")));
        model.put("status", "outer");
        model.put("currency", "KRW");

        String html = render("{{#orders}}{{#react}}StatusBadge status orderId=id currency order=this{{/react}}{{/orders}}");

        assertThat(html)
                .contains("data-mr-props=\"mr-0\">{\"status\":\"PAID\",\"orderId\":1,\"currency\":\"KRW\",\"order\":{\"id\":1,\"status\":\"PAID\"}}")
                .contains("data-mr-props=\"mr-1\">{\"status\":\"SHIPPED\",\"orderId\":2,\"currency\":\"KRW\",\"order\":{\"id\":2,\"status\":\"SHIPPED\"}}");
    }

    @Test
    void passesNullValuesButRejectsUnknownNames() {
        model.put("empty", null);

        assertThat(render("{{#react}}OrderChart empty{{/react}}")).contains("{\"empty\":null}");
        assertThatThrownBy(() -> render("{{#react}}OrderChart orders{{/react}}"))
                .hasMessageContaining("'orders' was not found")
                .hasMessageContaining("model.addAttribute(\"orders\", ...)");
        model.put("order", new Order(1, "PAID"));
        assertThatThrownBy(() -> render("{{#react}}OrderChart x=order.missing{{/react}}"))
                .hasMessageContaining("'order.missing' was not found")
                .hasMessageNotContaining("addAttribute");
    }

    @Test
    void scriptsPreloadTheIslandsUsedOnThePage() {
        model.put("orders", Collections.emptyList());

        String html = render("<head>{{{mr.head}}}</head>{{#react}}OrderChart orders{{/react}}"
                + "{{#react}}StatusBadge{{/react}}{{#react}}StatusBadge{{/react}}<!--scripts-->{{{mr.scripts}}}");

        String head = html.substring(0, html.indexOf("</head>"));
        String scripts = html.substring(html.indexOf("<!--scripts-->") + "<!--scripts-->".length());
        assertThat(head).contains("assets/mustache-react-2.css").doesNotContain("OrderChart");
        assertThat(scripts)
                .isEqualTo("<script type=\"application/json\" id=\"mr-context\">{\"locale\":\"ko-KR\"}</script>\n"
                        + "<link rel=\"stylesheet\" href=\"/mustache-react/assets/OrderChart-3.css\">\n"
                        + "<link rel=\"modulepreload\" href=\"/mustache-react/assets/OrderChart-3.js\">\n"
                        + "<link rel=\"modulepreload\" href=\"/mustache-react/assets/StatusBadge-4.js\">\n"
                        + "<script type=\"module\" src=\"/mustache-react/assets/mustache-react-2.js\"></script>\n");
    }

    @Test
    void rendersIslandsOnTheServerWithTheSameContextAsThePage() {
        java.util.concurrent.atomic.AtomicInteger contexts = new java.util.concurrent.atomic.AtomicInteger();
        ServerContextFactory context = new ServerContextFactory(
                java.util.Collections.<ServerContextContributor>singletonList(c -> c.put("n", contexts.incrementAndGet())),
                serializer);
        ServerSideRendering ssr = new ServerSideRendering(ServerSideRendering.Mode.AUTO,
                (component, props, json) -> "<b>" + component + json + "</b>");
        model.put("react", new ReactIslandLambda(serializer, ssr, context));
        model.put("mr", new MustacheReactHelpers(new ManifestFrontendAssets(manifest, "/mustache-react/"), context));

        String html = render("{{#react}}StatusBadge{{/react}}{{#react}}OrderChart{{/react}}{{{mr.scripts}}}");

        assertThat(html)
                .contains("data-mr-ssr=\"true\"><b>islands/StatusBadge{\"n\":1}</b></div>")
                .contains("data-mr-ssr=\"true\"><b>islands/OrderChart{\"n\":1}</b></div>")
                .contains("id=\"mr-context\">{\"n\":1}</script>");
        assertThat(contexts).hasValue(1);
    }

    @Test
    void idsAreUniqueWithinARequestAndRestartForTheNext() {
        assertThat(render("{{#react}}StatusBadge{{/react}}{{#react}}StatusBadge{{/react}}"))
                .contains("data-mr-id=\"mr-0\"").contains("data-mr-id=\"mr-1\"");

        RequestContextHolder.setRequestAttributes(new MapRequestAttributes());
        assertThat(render("{{#react}}StatusBadge{{/react}}")).contains("data-mr-id=\"mr-0\"");
    }

    @Test
    void worksOutsideARequestWithUniqueIds() {
        RequestContextHolder.resetRequestAttributes();

        String first = render("{{#react}}StatusBadge{{/react}}");
        String second = render("{{#react}}StatusBadge{{/react}}");

        assertThat(first).contains("data-mr-id=\"mr-x");
        assertThat(first.substring(0, first.indexOf('>'))).isNotEqualTo(second.substring(0, second.indexOf('>')));
    }
}
