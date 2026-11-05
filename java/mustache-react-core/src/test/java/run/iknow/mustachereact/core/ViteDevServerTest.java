package run.iknow.mustachereact.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ViteDevServerTest {

    private final ViteDevServer server = new ViteDevServer("http://localhost:5173/mustache-react");

    @Test
    void headInstallsReactRefreshBeforeTheViteClient() {
        String head = server.headTags();

        assertThat(head)
                .contains("import RefreshRuntime from \"http://localhost:5173/mustache-react/@react-refresh\";")
                .contains("window.__vite_plugin_react_preamble_installed__ = true;");
        assertThat(head.indexOf("@react-refresh")).isLessThan(head.indexOf("@vite/client"));
        assertThat(head).endsWith("<script type=\"module\" src=\"http://localhost:5173/mustache-react/@vite/client\"></script>\n");
    }

    @Test
    void scriptsLoadTheGeneratedEntry() {
        assertThat(server.scriptTags()).isEqualTo("<script type=\"module\" "
                + "src=\"http://localhost:5173/mustache-react/@id/__x00__virtual:mustache-react/entry\"></script>\n");
    }
}
