package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

class NodeSsrServerTest {

    @TempDir
    Path dir;

    private static boolean nodeAvailable() {
        try {
            return new ProcessBuilder("node", "--version").start().waitFor() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    void runsTheBundleAndStopsIt() throws Exception {
        assumeTrue(nodeAvailable(), "needs node on the PATH");
        Path bundle = dir.resolve("server.mjs");
        // Stands in for the bundle built by the vite plugin: echoes the component name.
        Files.write(bundle, ("import { createServer } from 'node:http';\n"
                + "const port = Number(process.argv[process.argv.indexOf('--port') + 1]);\n"
                + "createServer((req, res) => { let b = ''; req.on('data', c => b += c); req.on('end', () => {\n"
                + "  res.setHeader('Content-Type', 'application/json');\n"
                + "  res.end(JSON.stringify({ html: '<p>' + JSON.parse(b).component + '</p>' })); }); })\n"
                + "  .listen(port, '127.0.0.1');\n").getBytes(StandardCharsets.UTF_8));
        NodeSsrServer server = new NodeSsrServer(new DefaultResourceLoader(), bundle.toUri().toString(), "node", null,
                10_000, true);

        try {
            server.start();
            assertThat(server.isRunning()).isTrue();
            assertThat(new HttpSsrEngine(server::getEndpoint, 2000).render("pages/A", "{}", "{}")).isEqualTo("<p>pages/A</p>");
        } finally {
            server.stop();
        }
        assertThat(server.isRunning()).isFalse();
        assertThat(server.getEndpoint()).isNull();
    }

    @Test
    void withoutABundlePagesRenderInTheBrowserUnlessSsrIsRequired() {
        String missing = dir.resolve("missing.mjs").toUri().toString();

        NodeSsrServer optional = new NodeSsrServer(new DefaultResourceLoader(), missing, "node", null, 1000, false);
        optional.start();
        assertThat(optional.getEndpoint()).isNull();

        NodeSsrServer required = new NodeSsrServer(new DefaultResourceLoader(), missing, "node", null, 1000, true);
        assertThatThrownBy(required::start).hasMessageContaining("mustache-react.ssr=always").hasMessageContaining("no SSR bundle");
    }
}
