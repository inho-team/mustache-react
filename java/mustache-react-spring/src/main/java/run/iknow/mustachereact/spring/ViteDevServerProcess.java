package run.iknow.mustachereact.spring;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Starts the Vite dev server with the application, in development, and stops it with the application.
 *
 * <p>Runs {@code mustache-react-vite dev} from the frontend directory with the Node.js installed by the Gradle or
 * Maven plugin ({@code .mustache-react/node}), or the {@code node} on the {@code PATH}. If something already listens
 * on the dev server port (a dev server started by hand), that one is used and nothing is started.
 */
public class ViteDevServerProcess implements SmartLifecycle {

    private static final Log LOGGER = LogFactory.getLog(ViteDevServerProcess.class);

    /** The CLI, relative to the frontend directory. */
    static final String CLI = "node_modules/@mustache-react/vite-plugin/dist/bin.js";

    private final File frontendDir;
    private final URI url;
    private final String publicPath;
    private final String node;
    private final long startupTimeoutMillis;

    private volatile Process process;

    /**
     * @param frontendDir the frontend directory
     * @param url dev server origin, e.g. {@code http://localhost:5173}
     * @param publicPath public path, e.g. {@code /mustache-react/}
     * @param node the node executable, or {@code null} to look for one (see {@link #findNode(File)})
     * @param nodeInstallDir where the build plugins download Node.js, e.g. {@code .mustache-react/node}
     * @param startupTimeoutMillis how long to wait for the server to listen
     */
    public ViteDevServerProcess(File frontendDir, URI url, String publicPath, String node, File nodeInstallDir,
            long startupTimeoutMillis) {
        this.frontendDir = frontendDir;
        this.url = url;
        this.publicPath = publicPath;
        this.node = node != null && !node.isEmpty() ? node : findNode(nodeInstallDir);
        this.startupTimeoutMillis = startupTimeoutMillis;
    }

    /**
     * Finds the newest Node.js downloaded by the build plugins, or falls back to {@code node} on the {@code PATH}.
     *
     * @param installDir the download directory, may be {@code null}
     * @return the node command
     */
    static String findNode(File installDir) {
        File[] versions = installDir == null ? null : installDir.listFiles(file -> file.getName().startsWith("node-v"));
        if (versions != null && versions.length > 0) {
            Arrays.sort(versions, (a, b) -> compareVersions(b.getName(), a.getName()));
            for (File version : versions) {
                for (String candidate : new String[] {"bin/node", "node.exe"}) {
                    File executable = new File(version, candidate);
                    if (executable.isFile()) {
                        return executable.getAbsolutePath();
                    }
                }
            }
        }
        return "node";
    }

    /** Compares {@code node-v24.10.0-linux-x64} style names by version numbers. */
    static int compareVersions(String a, String b) {
        String[] x = a.replaceAll("^node-v|-.*$", "").split("\\.");
        String[] y = b.replaceAll("^node-v|-.*$", "").split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int left = i < x.length ? parse(x[i]) : 0;
            int right = i < y.length ? parse(y[i]) : 0;
            if (left != right) {
                return Integer.compare(left, right);
            }
        }
        return 0;
    }

    private static int parse(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @Override
    public void start() {
        String host = url.getHost() == null ? "localhost" : url.getHost();
        int port = url.getPort() > 0 ? url.getPort() : 5173;
        if (isListening(host, port)) {
            LOGGER.info("mustache-react: using the Vite dev server already running at " + url);
            return;
        }
        File cli = new File(frontendDir, CLI);
        if (!cli.isFile()) {
            LOGGER.warn("mustache-react: cannot start the Vite dev server: " + cli.getAbsolutePath() + " not found. "
                    + "Install the frontend dependencies (./gradlew mustacheReactInstall, or mvn generate-resources), "
                    + "or set mustache-react.frontend-dir.");
            return;
        }
        List<String> command = new ArrayList<>(Arrays.asList(node, cli.getAbsolutePath(), "dev", "--port",
                String.valueOf(port), "--base", publicPath));
        ProcessBuilder builder = new ProcessBuilder(command).directory(frontendDir).inheritIO();
        prependToPath(builder.environment(), node);
        LOGGER.info("mustache-react: starting the Vite dev server: " + String.join(" ", command));
        try {
            process = builder.start();
        } catch (IOException ex) {
            LOGGER.warn("mustache-react: could not start the Vite dev server with '" + node + "': " + ex.getMessage()
                    + ". Install Node.js, or let the build plugin download it.");
            return;
        }
        long deadline = System.currentTimeMillis() + startupTimeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                LOGGER.warn("mustache-react: the Vite dev server exited with code " + process.exitValue());
                process = null;
                return;
            }
            if (isListening(host, port)) {
                LOGGER.info("mustache-react: Vite dev server ready at " + url);
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        LOGGER.warn("mustache-react: the Vite dev server did not listen on " + host + ":" + port + " within "
                + startupTimeoutMillis + " ms; pages may not load their scripts yet.");
    }

    private static void prependToPath(Map<String, String> environment, String node) {
        File executable = new File(node);
        if (!executable.isAbsolute()) {
            return;
        }
        String key = "PATH";
        for (String name : environment.keySet()) {
            if (name.equalsIgnoreCase("PATH")) {
                key = name;
            }
        }
        String existing = environment.get(key);
        String bin = executable.getParent();
        environment.put(key, existing == null || existing.isEmpty() ? bin : bin + File.pathSeparator + existing);
    }

    static boolean isListening(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 200);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public void stop() {
        Process current = process;
        process = null;
        if (current == null) {
            return;
        }
        current.destroy();
        try {
            if (!current.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                current.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            current.destroyForcibly();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("mustache-react: stopped the Vite dev server");
    }

    @Override
    public boolean isRunning() {
        Process current = process;
        return current != null && current.isAlive();
    }

    /** @return the node command used */
    public String getNode() {
        return node;
    }
}
