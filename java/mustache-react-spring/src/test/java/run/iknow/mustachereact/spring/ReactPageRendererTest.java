package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.samskivert.mustache.Mustache;

class ReactPageRendererTest {

    private static final String MANIFEST = "classpath:/renderer/manifest.json";

    private ReactPageRenderer renderer(String manifest, String layout) {
        DefaultResourceLoader loader = new DefaultResourceLoader();
        FrontendAssets assets = new ManifestFrontendAssets(new ViteManifestSource(loader, manifest), "/mustache-react/");
        return new ReactPageRenderer(loader, Mustache.compiler(), assets, context(), ServerSideRendering.off(), layout, true);
    }

    /** Server context with a fixed entry, serialized by a minimal Map writer. */
    static ServerContextFactory context() {
        return new ServerContextFactory(java.util.Collections.<ServerContextContributor>singletonList(c -> c.put("locale", "ko-KR")),
                new PropsSerializer(java.util.Collections.<org.springframework.http.converter.HttpMessageConverter<?>>singletonList(
                        new ReactIslandLambdaTest.MapJsonConverter())));
    }

    @Test
    void rendersTheBuiltInLayout() {
        String html = renderer(MANIFEST, "classpath:/does-not-exist.mustache")
                .render("orders/OrderList", "", "{\"total\":2}", Locale.KOREA);

        assertThat(html)
                .startsWith("<!doctype html>")
                .contains("<html lang=\"ko-KR\">")
                .contains("<title>orders/OrderList</title>")
                .contains("<link rel=\"stylesheet\" href=\"/mustache-react/assets/mustache-react-2.css\">")
                .contains("<link rel=\"modulepreload\" href=\"/mustache-react/assets/OrderList-1.js\">")
                .contains("<div data-mr-island=\"pages/orders/OrderList\" data-mr-id=\"mr-page\"></div>")
                .contains("<script type=\"application/json\" data-mr-props=\"mr-page\">{\"total\":2}</script>")
                .contains("<script type=\"application/json\" id=\"mr-context\">{\"locale\":\"ko-KR\"}</script>\n<script type=\"module\"")
                .contains("<script type=\"module\" src=\"/mustache-react/assets/mustache-react-2.js\"></script>");
    }

    @Test
    void usesTheApplicationLayoutAndEscapesTheTitle() {
        String html = renderer(MANIFEST, "classpath:/renderer/templates/mustache-react/layout.mustache")
                .render("orders/OrderList", "<주문>", "{}", Locale.ENGLISH);

        assertThat(html).contains("<title>&lt;주문&gt; | Custom</title>").contains("data-mr-island=\"pages/orders/OrderList\"");
    }

    @Test
    void explainsAMissingManifest() {
        ReactPageRenderer renderer = renderer("classpath:/missing/manifest.json", ReactPageRenderer.DEFAULT_LAYOUT);

        assertThatThrownBy(() -> renderer.render("orders/OrderList", "", "{}", Locale.ENGLISH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Vite manifest not found")
                .hasMessageContaining("mustache-react.manifest-location");
    }

    @Test
    void loadsFromTheDevServerWithoutAManifest() {
        DefaultResourceLoader loader = new DefaultResourceLoader();
        ReactPageRenderer renderer = new ReactPageRenderer(loader, Mustache.compiler(),
                new DevServerFrontendAssets(new run.iknow.mustachereact.core.ViteDevServer("http://localhost:5173/mustache-react/")),
                context(), ServerSideRendering.off(), "classpath:/does-not-exist.mustache", true);

        String html = renderer.render("orders/OrderList", "", "{}", Locale.ENGLISH);

        assertThat(html)
                .contains("http://localhost:5173/mustache-react/@react-refresh")
                .contains("<script type=\"module\" src=\"http://localhost:5173/mustache-react/@vite/client\"></script>")
                .contains("src=\"http://localhost:5173/mustache-react/@id/__x00__virtual:mustache-react/entry\"")
                .doesNotContain("modulepreload");
    }

    @Test
    void addsExtraContextEntriesSuchAsValidationErrors() {
        String html = renderer(MANIFEST, ReactPageRenderer.DEFAULT_LAYOUT).render("orders/OrderList", "", "{}",
                java.util.Collections.<String, Object>singletonMap("errors", "x"), Locale.ENGLISH);

        assertThat(html).contains("id=\"mr-context\">{\"locale\":\"ko-KR\",\"errors\":\"x\"}</script>");
    }

    @Test
    void embedsServerRenderedHtmlForHydration() {
        DefaultResourceLoader loader = new DefaultResourceLoader();
        FrontendAssets assets = new ManifestFrontendAssets(new ViteManifestSource(loader, MANIFEST), "/mustache-react/");
        ServerSideRendering ssr = new ServerSideRendering(ServerSideRendering.Mode.AUTO,
                (component, props, context) -> "<h1>" + component + "</h1><!--" + context + "-->");
        ReactPageRenderer renderer = new ReactPageRenderer(loader, Mustache.compiler(), assets, context(), ssr,
                ReactPageRenderer.DEFAULT_LAYOUT, true);

        String html = renderer.render("orders/OrderList", "", "{}", Locale.ENGLISH);

        assertThat(html).contains("<div data-mr-island=\"pages/orders/OrderList\" data-mr-id=\"mr-page\" data-mr-ssr=\"true\">"
                + "<h1>pages/orders/OrderList</h1><!--{\"locale\":\"ko-KR\"}--></div>");
    }

    @Test
    void rejectsPageNamesThatEscapeThePagesDirectory() {
        assertThatThrownBy(() -> renderer(MANIFEST, ReactPageRenderer.DEFAULT_LAYOUT).render("../secret", "", "{}", Locale.ENGLISH))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
