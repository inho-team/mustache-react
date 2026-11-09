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
    void loadsAssetsFromACdnWithCors() {
        PageAssets assets = ViteManifest.parse(MANIFEST).assetsFor("pages/orders/OrderList");

        assertThat(assets.headTags("https://cdn.example.com/shop"))
                .contains("<link rel=\"stylesheet\" href=\"https://cdn.example.com/shop/assets/OrderList-3.css\" crossorigin>")
                .contains("<link rel=\"modulepreload\" href=\"https://cdn.example.com/shop/assets/OrderList-3.js\" crossorigin>");
        assertThat(assets.scriptTags("//cdn.example.com/shop/")).isEqualTo(
                "<script type=\"module\" src=\"//cdn.example.com/shop/assets/mustache-react-4.js\" crossorigin></script>\n");
        assertThat(PageAssets.isAbsolute("/mustache-react/")).isFalse();
        assertThat(PageAssets.isAbsolute("HTTPS://cdn")).isTrue();
    }

    @Test
    void collectsSeveralComponentsAndSplitsHeadFromBody() {
        ViteManifest manifest = ViteManifest.parse(MANIFEST);
        PageAssets head = manifest.entryAssets();
        PageAssets all = manifest.assetsForComponents(java.util.Arrays.asList("pages/orders/OrderList", "pages/Missing"));

        PageAssets body = all.without(head);

        // The entry imports the vendor chunk, so its CSS is already in <head>.
        assertThat(head.getStyles()).containsExactly("assets/mustache-react-4.css", "assets/vendor-1.css");
        assertThat(head.getPreloads()).containsExactly("assets/vendor-1.js");
        assertThat(body.getScripts()).containsExactly("assets/mustache-react-4.js");
        assertThat(body.getStyles()).containsExactly("assets/OrderList-3.css");
        assertThat(body.getPreloads()).containsExactly("assets/OrderList-3.js", "assets/shared-2.js");
    }

    @Test
    void failsClearlyWithoutTheEntry() {
        ViteManifest manifest = ViteManifest.parse("{\"main.tsx\": {\"file\": \"assets/main.js\", \"isEntry\": true}}");

        assertThatThrownBy(manifest::entryAssets)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("@mustache-react/vite-plugin");
    }
}
