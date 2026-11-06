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
import java.util.concurrent.atomic.AtomicBoolean;

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
 * executable, not {@code node_modules}. A process that dies is restarted on a later request.
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
    private volatile Path script;
    private volatile boolean started;

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

    /**
     * @return the render URL, or {@code null} while the server is not running. When the Node.js process has died, a
     *         restart is started in the background (at most every {@link #RESTART_INTERVAL_MILLIS} ms); requests
     *         render in the browser meanwhile.
     */
    public String getEndpoint() {
        Process current = process;
        if (current != null && current.isAlive()) {
            return endpoint;
        }
        if (started && script != null) {
            scheduleRestart(current);
        }
        return null;
    }

    /** Minimum time between two restarts of a crashed SSR server. */
    static final long RESTART_INTERVAL_MILLIS = 10_000;

    private long restartIntervalMillis = RESTART_INTERVAL_MILLIS;
    private final AtomicBoolean restarting = new AtomicBoolean();
    private volatile long lastRestart;

    /** For tests. */
    void setRestartIntervalMillis(long millis) {
        this.restartIntervalMillis = millis;
    }

    /** For tests: the running process. */
    Process process() {
        return process;
    }

    private void scheduleRestart(Process dead) {
        long now = System.currentTimeMillis();
        if (now - lastRestart < restartIntervalMillis || !restarting.compareAndSet(false, true)) {
            return;
        }
        lastRestart = now;
        LOGGER.warn("mustache-react: the SSR server " + (dead == null ? "is not running" : "exited with code "
                + exitCode(dead)) + "; restarting it (pages render in the browser meanwhile)");
        Thread thread = new Thread(() -> {
            try {
                launch(false);
            } finally {
                restarting.set(false);
            }
        }, "mustache-react-ssr-restart");
        thread.setDaemon(true);
        thread.start();
    }

    private static String exitCode(Process process) {
        try {
            return String.valueOf(process.exitValue());
        } catch (IllegalThreadStateException ex) {
            return "?";
        }
    }

    @Override
    public void start() {
        started = true;
        Resource bundle = resourceLoader.getResource(bundleLocation);
        if (!bundle.exists()) {
            unavailable("no SSR bundle at " + bundleLocation + ". Enable ssr in the mustache-react Gradle or Maven "
                    + "plugin to build it.", null);
            return;
        }
        try {
            workDir = Files.createTempDirectory("mustache-react-ssr");
            Path extracted = workDir.resolve("server.mjs");
            try (InputStream in = bundle.getInputStream()) {
                Files.copy(in, extracted, StandardCopyOption.REPLACE_EXISTING);
            }
            script = extracted;
        } catch (IOException ex) {
            unavailable("could not extract the SSR bundle: " + ex.getMessage(), ex);
            return;
        }
        launch(true);
    }

    /** Starts the Node.js process on a free port and waits until it listens. */
    private void launch(boolean initial) {
        if (!started) {
            return; // stopped while a restart was pending
        }
        try {
            int port = freePort();
            List<String> command = new ArrayList<>(Arrays.asList(node, script.toString(), "--port", String.valueOf(port)));
            LOGGER.info("mustache-react: starting the SSR server: " + String.join(" ", command));
            // Clear the old port first: once the new process is alive, getEndpoint() must not hand out the port of
            // the dead one before the new one listens.
            endpoint = null;
            Process started = new ProcessBuilder(command).directory(workDir.toFile()).inheritIO().start();
            process = started;
            if (!this.started) {
                // stop() ran while this restart was starting the process: do not leave it behind.
                started.destroyForcibly();
                process = null;
                return;
            }
            long deadline = System.currentTimeMillis() + startupTimeoutMillis;
            while (System.currentTimeMillis() < deadline) {
                if (!started.isAlive()) {
                    failed(initial, "the SSR server exited with code " + started.exitValue(), null);
                    return;
                }
                if (ViteDevServerProcess.isListening("127.0.0.1", port)) {
                    endpoint = "http://127.0.0.1:" + port + HttpSsrEngine.RENDER_PATH;
                    LOGGER.info("mustache-react: SSR server ready on 127.0.0.1:" + port);
                    return;
                }
                Thread.sleep(50);
            }
            failed(initial, "the SSR server did not start within " + startupTimeoutMillis + " ms", null);
        } catch (IOException ex) {
            failed(initial, "could not start the SSR server with '" + node + "': " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void failed(boolean initial, String reason, Exception cause) {
        if (initial) {
            unavailable(reason, cause);
        } else {
            LOGGER.warn("mustache-react: " + reason + "; retrying on a later request");
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
        started = false;
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
        script = null;
        if (dir != null) {
            try {
                Files.deleteIfExists(dir.resolve("server.mjs"));
                Files.deleteIfExists(dir);
            } catch (IOException ex) {
                LOGGER.debug("mustache-react: could not delete " + dir, ex);
            }
        }
    }

    /** Running from {@link #start()} to {@link #stop()}, also while a crashed process is being restarted. */
    @Override
    public boolean isRunning() {
        return started;
    }
}
