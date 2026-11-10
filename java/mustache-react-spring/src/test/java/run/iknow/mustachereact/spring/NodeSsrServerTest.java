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
        // Stands in for the bundle built by the vite plugin: echoes the component name, and like the real server
        // refuses requests without the token from its environment.
        Files.write(bundle, ("import { createServer } from 'node:http';\n"
                + "const port = Number(process.argv[process.argv.indexOf('--port') + 1]);\n"
                + "createServer((req, res) => { if (!process.env.MUSTACHE_REACT_SSR_TOKEN\n"
                + "    || req.headers['x-mustache-react-token'] !== process.env.MUSTACHE_REACT_SSR_TOKEN) {\n"
                + "  res.writeHead(403); res.end(); return; }\n"
                + "  let b = ''; req.on('data', c => b += c); req.on('end', () => {\n"
                + "  res.setHeader('Content-Type', 'application/json');\n"
                + "  res.end(JSON.stringify({ html: '<p>' + JSON.parse(b).component + '</p>' })); }); })\n"
                + "  .listen(port, '127.0.0.1');\n").getBytes(StandardCharsets.UTF_8));
        NodeSsrServer server = new NodeSsrServer(new DefaultResourceLoader(), bundle.toUri().toString(), "node", null,
                10_000, true);

        try {
            server.start();
            assertThat(server.isRunning()).isTrue();
            assertThat(new HttpSsrEngine(server::getEndpoint, 2000, server::getToken).render("pages/A", "{}", "{}"))
                    .isEqualTo("<p>pages/A</p>");
            assertThat(server.getToken()).hasSize(64);
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> new HttpSsrEngine(server::getEndpoint, 2000).render("pages/A", "{}", "{}"))
                    .hasMessageContaining("HTTP 403");
        } finally {
            server.stop();
        }
        assertThat(server.isRunning()).isFalse();
        assertThat(server.getEndpoint()).isNull();
    }

    @Test
    void restartsACrashedServer() throws Exception {
        assumeTrue(nodeAvailable(), "needs node on the PATH");
        Path bundle = dir.resolve("server.mjs");
        Files.write(bundle, ("import { createServer } from 'node:http';\n"
                + "const port = Number(process.argv[process.argv.indexOf('--port') + 1]);\n"
                + "createServer((req, res) => res.end(JSON.stringify({ html: 'pid ' + process.pid }))).listen(port, '127.0.0.1');\n")
                .getBytes(StandardCharsets.UTF_8));
        NodeSsrServer server = new NodeSsrServer(new DefaultResourceLoader(), bundle.toUri().toString(), "node", null,
                10_000, false);
        server.setRestartIntervalMillis(0);
        try {
            server.start();
            String first = server.getEndpoint();
            assertThat(first).isNotNull();

            server.process().destroyForcibly().waitFor();
            assertThat(server.getEndpoint()).as("falls back while restarting").isNull();
            assertThat(server.isRunning()).as("still running for the lifecycle").isTrue();

            String restarted = null;
            for (int i = 0; i < 100 && restarted == null; i++) {
                Thread.sleep(100);
                restarted = server.getEndpoint();
            }
            assertThat(restarted).as("restarted within 10 s").isNotNull();
            assertThat(new HttpSsrEngine(server::getEndpoint, 2000).render("pages/A", "{}", "{}")).startsWith("pid ");
        } finally {
            server.stop();
        }
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
