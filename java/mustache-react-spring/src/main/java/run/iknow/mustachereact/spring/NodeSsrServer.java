package run.iknow.mustachereact.spring;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Runs the SSR bundle built by the frontend build ({@code META-INF/mustache-react/ssr/server.mjs} in the jar) as a
 * Node.js process next to the application, on a free port of 127.0.0.1, for as long as the application runs.
 *
 * <p>The bundle contains React and the application's components, so the server needs only a {@code node}
 * executable, not {@code node_modules}.
 */
public class NodeSsrServer implements SmartLifecycle {

    private static final Log LOGGER = LogFactory.getLog(NodeSsrServer.class);

    private final ResourceLoader resourceLoader;
    private final String bundleLocation;
    private final String node;
    private final long startupTimeoutMillis;
    private final boolean required;

    private volatile Process process;
    private volatile String endpoint;
    private volatile Path workDir;

    /**
     * @param resourceLoader loads the bundle
     * @param bundleLocation location of {@code server.mjs}
     * @param node node executable, or {@code null} to look in {@code nodeInstallDir} and on the PATH
     * @param nodeInstallDir where the build plugins download Node.js, may be {@code null}
     * @param startupTimeoutMillis how long to wait for the server to listen
     * @param required whether a missing bundle or a failed start must stop the application ({@code ssr=always})
     */
    public NodeSsrServer(ResourceLoader resourceLoader, String bundleLocation, String node, File nodeInstallDir,
            long startupTimeoutMillis, boolean required) {
        this.resourceLoader = resourceLoader;
        this.bundleLocation = bundleLocation;
        this.node = node != null && !node.isEmpty() ? node : ViteDevServerProcess.findNode(nodeInstallDir);
        this.startupTimeoutMillis = startupTimeoutMillis;
        this.required = required;
    }

    /** @return the render URL, or {@code null} while the server is not running */
    public String getEndpoint() {
        Process current = process;
        return current != null && current.isAlive() ? endpoint : null;
    }

    @Override
    public void start() {
        Resource bundle = resourceLoader.getResource(bundleLocation);
        if (!bundle.exists()) {
            unavailable("no SSR bundle at " + bundleLocation + ". Enable ssr in the mustache-react Gradle or Maven "
                    + "plugin to build it.", null);
            return;
        }
        try {
            workDir = Files.createTempDirectory("mustache-react-ssr");
            Path script = workDir.resolve("server.mjs");
            try (InputStream in = bundle.getInputStream()) {
                Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
            }
            int port = freePort();
            List<String> command = new ArrayList<>(Arrays.asList(node, script.toString(), "--port", String.valueOf(port)));
            LOGGER.info("mustache-react: starting the SSR server: " + String.join(" ", command));
            process = new ProcessBuilder(command).directory(workDir.toFile()).inheritIO().start();
            long deadline = System.currentTimeMillis() + startupTimeoutMillis;
            while (System.currentTimeMillis() < deadline) {
                if (!process.isAlive()) {
                    unavailable("the SSR server exited with code " + process.exitValue(), null);
                    process = null;
                    return;
                }
                if (ViteDevServerProcess.isListening("127.0.0.1", port)) {
                    endpoint = "http://127.0.0.1:" + port + HttpSsrEngine.RENDER_PATH;
                    LOGGER.info("mustache-react: SSR server ready on 127.0.0.1:" + port);
                    return;
                }
                Thread.sleep(50);
            }
            unavailable("the SSR server did not start within " + startupTimeoutMillis + " ms", null);
        } catch (IOException ex) {
            unavailable("could not start the SSR server with '" + node + "': " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void unavailable(String reason, Exception cause) {
        if (required) {
            throw new IllegalStateException("mustache-react.ssr=always, but " + reason, cause);
        }
        LOGGER.warn("mustache-react: " + reason + " Pages render in the browser.");
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Override
    public void stop() {
        Process current = process;
        process = null;
        endpoint = null;
        if (current != null) {
            current.destroy();
            try {
                if (!current.waitFor(5, TimeUnit.SECONDS)) {
                    current.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                current.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
        Path dir = workDir;
        workDir = null;
        if (dir != null) {
            try {
                Files.deleteIfExists(dir.resolve("server.mjs"));
                Files.deleteIfExists(dir);
            } catch (IOException ex) {
                LOGGER.debug("mustache-react: could not delete " + dir, ex);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return getEndpoint() != null;
    }
}
