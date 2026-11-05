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
        return new ReactPageRenderer(loader, Mustache.compiler(), new ViteManifestSource(loader, manifest), layout,
                "/mustache-react/", true);
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
    void rejectsPageNamesThatEscapeThePagesDirectory() {
        assertThatThrownBy(() -> renderer(MANIFEST, ReactPageRenderer.DEFAULT_LAYOUT).render("../secret", "", "{}", Locale.ENGLISH))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
