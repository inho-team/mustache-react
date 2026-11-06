package run.iknow.mustachereact.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

class NodeInstallerTest {

    private static final String VERSION = "24.0.0";
    private static final Platform LINUX = Platform.of("Linux", "amd64");

    @TempDir
    Path dir;

    private HttpServer server;
    private final Map<String, byte[]> files = new HashMap<>();
    private final List<String> requests = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requests.add(path);
            byte[] body = files.get(path);
            exchange.sendResponseHeaders(body == null ? 404 : 200, body == null ? -1 : body.length);
            if (body != null) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/dist/";
    }

    private void publish(String checksumOverride) throws Exception {
        String name = LINUX.distributionName(VERSION);
        Path archive = new TarFixtures()
                .file(name + "/bin/node", "#!/bin/sh\necho v" + VERSION + "\n", 0755)
                .file(name + "/lib/node_modules/npm/bin/npm-cli.js", "", 0644)
                .writeTo(dir.resolve("archive.tar.gz"));
        byte[] bytes = Files.readAllBytes(archive);
        StringBuilder sha = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) {
            sha.append(String.format("%02x", b));
        }
        String checksum = checksumOverride != null ? checksumOverride : sha.toString();
        files.put("/dist/v" + VERSION + "/" + LINUX.archiveName(VERSION), bytes);
        files.put("/dist/v" + VERSION + "/SHASUMS256.txt",
                ("0000  node-v" + VERSION + "-darwin-arm64.tar.gz\n" + checksum + "  " + LINUX.archiveName(VERSION) + "\n")
                        .getBytes("UTF-8"));
    }

    @Test
    void downloadsVerifiesAndReusesTheInstallation() throws Exception {
        assumeFalse(Platform.current().isWindows());
        publish(null);
        NodeInstaller installer = new NodeInstaller(dir.resolve(".mustache-react/node"), baseUrl(), LINUX, BuildLog.STANDARD);

        NodeRuntime runtime = installer.install("v" + VERSION);

        assertThat(runtime.node()).isEqualTo(dir.resolve(".mustache-react/node/node-v24.0.0-linux-x64/bin/node"));
        assertThat(Files.isExecutable(runtime.node())).isTrue();
        assertThat(runtime.npm("ci")).containsExactly(runtime.node().toString(),
                dir.resolve(".mustache-react/node/node-v24.0.0-linux-x64/lib/node_modules/npm/bin/npm-cli.js").toString(), "ci");
        try (java.util.stream.Stream<Path> leftovers = Files.list(dir.resolve(".mustache-react/node"))) {
            assertThat(leftovers.map(p -> p.getFileName().toString())).containsExactly("node-v24.0.0-linux-x64");
        }

        int downloads = requests.size();
        installer.install(VERSION);
        assertThat(requests).hasSize(downloads);
    }

    @Test
    void refusesAnArchiveWithTheWrongChecksum() throws Exception {
        publish("deadbeef");
        NodeInstaller installer = new NodeInstaller(dir.resolve("node"), baseUrl(), LINUX, BuildLog.STANDARD);

        assertThatThrownBy(() -> installer.install(VERSION)).hasMessageContaining("Checksum mismatch");
        assertThat(dir.resolve("node/node-v24.0.0-linux-x64")).doesNotExist();
    }

    @Test
    void reportsMissingReleases() {
        NodeInstaller installer = new NodeInstaller(dir.resolve("node"), baseUrl(), LINUX, BuildLog.STANDARD);

        assertThatThrownBy(() -> installer.install("99.0.0")).hasMessageContaining("HTTP 404");
    }
}
