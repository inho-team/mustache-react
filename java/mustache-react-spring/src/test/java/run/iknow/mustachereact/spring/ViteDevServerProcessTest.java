package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ViteDevServerProcessTest {

    @TempDir
    Path dir;

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static boolean nodeAvailable() {
        try {
            return new ProcessBuilder("node", "--version").start().waitFor() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    void startsTheDevServerAndStopsItWithTheApplication() throws Exception {
        assumeTrue(nodeAvailable(), "needs node on the PATH");
        Path cli = dir.resolve("frontend/" + ViteDevServerProcess.CLI);
        Files.createDirectories(cli.getParent());
        // Stands in for mustache-react-vite: listens on --port.
        Files.write(cli, ("const port = Number(process.argv[process.argv.indexOf('--port') + 1]);\n"
                + "require('http').createServer((q, r) => r.end('ok')).listen(port, 'localhost');\n")
                .getBytes(StandardCharsets.UTF_8));
        int port = freePort();
        ViteDevServerProcess server = new ViteDevServerProcess(dir.resolve("frontend").toFile(),
                URI.create("http://localhost:" + port), "/mustache-react/", "node", null, 10_000);

        try {
            server.start();
            assertThat(server.isRunning()).isTrue();
            assertThat(ViteDevServerProcess.isListening("localhost", port)).isTrue();
        } finally {
            // Always: a node process left behind keeps the test worker's output open and the build never ends.
            server.stop();
        }
        assertThat(server.isRunning()).isFalse();
        // Windows releases the socket of a killed process shortly after the process has exited.
        long deadline = System.currentTimeMillis() + 5_000;
        while (ViteDevServerProcess.isListening("localhost", port) && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        assertThat(ViteDevServerProcess.isListening("localhost", port)).isFalse();
    }

    @Test
    void seesServersOnIpv6OnlyLocalhost() throws Exception {
        java.net.InetAddress ipv6 = java.net.InetAddress.getByName("::1");
        try (ServerSocket socket = new ServerSocket(0, 50, ipv6)) {
            assertThat(ViteDevServerProcess.isListening("localhost", socket.getLocalPort())).isTrue();
        } catch (java.net.SocketException ex) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "no IPv6 loopback: " + ex.getMessage());
        }
    }

    @Test
    void reusesARunningDevServer() throws Exception {
        try (ServerSocket running = new ServerSocket(0)) {
            ViteDevServerProcess server = new ViteDevServerProcess(dir.toFile(),
                    URI.create("http://localhost:" + running.getLocalPort()), "/mustache-react/", "node", null, 1_000);

            server.start();

            assertThat(server.isRunning()).isFalse();
        }
    }

    @Test
    void doesNotStartWithoutTheCli() throws Exception {
        ViteDevServerProcess server = new ViteDevServerProcess(dir.toFile(),
                URI.create("http://localhost:" + freePort()), "/mustache-react/", "node", null, 1_000);

        server.start();

        assertThat(server.isRunning()).isFalse();
    }

    @Test
    void prefersTheNewestDownloadedNode() throws Exception {
        for (String version : new String[] {"node-v9.11.0-linux-x64", "node-v24.2.0-linux-x64", "node-v24.10.0-linux-x64"}) {
            Path node = dir.resolve(version + "/bin/node");
            Files.createDirectories(node.getParent());
            Files.write(node, new byte[0]);
        }

        assertThat(ViteDevServerProcess.findNode(dir.toFile()))
                .isEqualTo(dir.resolve("node-v24.10.0-linux-x64/bin/node").toFile().getAbsolutePath());
        assertThat(ViteDevServerProcess.findNode(new File(dir.toFile(), "missing"))).isEqualTo("node");
        assertThat(ViteDevServerProcess.findNode(null)).isEqualTo("node");
    }

    @Test
    void tellsAViteDevServerFromAnotherProgramOnThePort() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mustache-react/@vite/client", exchange -> {
            byte[] body = "export {}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/javascript");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            assertThat(ViteDevServerProcess.isViteDevServer(url, "/mustache-react/")).isTrue();
            // Another program answers, but not with the Vite client.
            assertThat(ViteDevServerProcess.isViteDevServer(url, "/other/")).isFalse();
        } finally {
            server.stop(0);
        }
        assertThat(ViteDevServerProcess.isViteDevServer(URI.create("http://127.0.0.1:" + freePort()), "/mustache-react/"))
                .isFalse();
    }
}
