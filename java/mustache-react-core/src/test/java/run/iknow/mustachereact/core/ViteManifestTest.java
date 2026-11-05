package run.iknow.mustachereact.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ViteManifestTest {

    // Shape produced by Vite 8 with @mustache-react/vite-plugin, plus a shared chunk.
    private static final String MANIFEST = "{\n"
            + "  \"_vendor-1.js\": {\"file\": \"assets/vendor-1.js\", \"name\": \"vendor\", \"css\": [\"assets/vendor-1.css\"]},\n"
            + "  \"_shared-2.js\": {\"file\": \"assets/shared-2.js\", \"name\": \"shared\", \"imports\": [\"_vendor-1.js\"]},\n"
            + "  \"pages/orders/OrderList.tsx\": {\n"
            + "    \"file\": \"assets/OrderList-3.js\", \"name\": \"OrderList\", \"src\": \"pages/orders/OrderList.tsx\",\n"
            + "    \"isDynamicEntry\": true, \"imports\": [\"virtual:mustache-react/entry\", \"_shared-2.js\"],\n"
            + "    \"css\": [\"assets/OrderList-3.css\"]\n"
            + "  },\n"
            + "  \"virtual:mustache-react/entry\": {\n"
            + "    \"file\": \"assets/mustache-react-4.js\", \"name\": \"mustache-react\", \"src\": \"virtual:mustache-react/entry\",\n"
            + "    \"isEntry\": true, \"imports\": [\"_vendor-1.js\"], \"dynamicImports\": [\"pages/orders/OrderList.tsx\"],\n"
            + "    \"css\": [\"assets/mustache-react-4.css\"]\n"
            + "  }\n"
            + "}";

    @Test
    void collectsEntryAndPageAssets() {
        PageAssets assets = ViteManifest.parse(MANIFEST).assetsFor("pages/orders/OrderList");

        assertThat(assets.getScripts()).containsExactly("assets/mustache-react-4.js");
        assertThat(assets.getStyles()).containsExactly(
                "assets/mustache-react-4.css", "assets/vendor-1.css", "assets/OrderList-3.css");
        assertThat(assets.getPreloads()).containsExactly(
                "assets/vendor-1.js", "assets/OrderList-3.js", "assets/shared-2.js");
    }

    @Test
    void unknownComponentFallsBackToEntryOnly() {
        PageAssets assets = ViteManifest.parse(MANIFEST).assetsFor("pages/Missing");

        assertThat(assets.getScripts()).containsExactly("assets/mustache-react-4.js");
        assertThat(assets.getPreloads()).containsExactly("assets/vendor-1.js");
    }

    @Test
    void rendersTagsUnderThePublicPath() {
        PageAssets assets = ViteManifest.parse(MANIFEST).assetsFor("pages/orders/OrderList");

        assertThat(assets.headTags("/mustache-react/"))
                .contains("<link rel=\"stylesheet\" href=\"/mustache-react/assets/OrderList-3.css\">")
                .contains("<link rel=\"modulepreload\" href=\"/mustache-react/assets/OrderList-3.js\">");
        assertThat(assets.scriptTags("/static"))
                .isEqualTo("<script type=\"module\" src=\"/static/assets/mustache-react-4.js\"></script>\n");
    }

    @Test
    void failsClearlyWithoutTheEntry() {
        ViteManifest manifest = ViteManifest.parse("{\"main.tsx\": {\"file\": \"assets/main.js\", \"isEntry\": true}}");

        assertThatThrownBy(() -> manifest.assetsFor(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("@mustache-react/vite-plugin");
    }
}
