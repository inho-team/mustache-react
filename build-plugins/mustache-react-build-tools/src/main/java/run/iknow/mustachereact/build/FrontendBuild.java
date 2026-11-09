package run.iknow.mustachereact.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Installs the dependencies of a frontend directory and builds it with {@code mustache-react-vite}
 * (the CLI of {@code @mustache-react/vite-plugin}).
 */
public final class FrontendBuild {

    /** The CLI, relative to the frontend directory. */
    static final String CLI = "node_modules/@mustache-react/vite-plugin/dist/bin.js";

    /** Where the SSR bundle goes in the resources: {@code classpath:/META-INF/mustache-react/ssr/server.mjs}. */
    public static final String SSR_RESOURCE_DIR = "META-INF/mustache-react/ssr";

    private final Path frontendDir;
    private final NodeRuntime node;
    private final BuildLog log;

    /**
     * @param frontendDir directory holding {@code package.json}, e.g. {@code src/main/frontend}
     * @param node the Node.js installation to use
     * @param log build log
     */
    public FrontendBuild(Path frontendDir, NodeRuntime node, BuildLog log) {
        this.frontendDir = frontendDir;
        this.node = node;
        this.log = log;
    }

    /**
     * Runs the package manager's install.
     *
     * @param packageManager {@code auto}, {@code npm}, {@code pnpm} or {@code yarn}
     * @throws IOException if {@code package.json} is missing or the install fails
     */
    public void install(String packageManager) throws IOException {
        requirePackageJson();
        PackageManager manager = PackageManager.detect(frontendDir, packageManager);
        List<String> command = manager.installCommand(node);
        log.info("mustache-react: installing frontend dependencies with " + manager);
        ProcessRunner.run(command, frontendDir, environment(), log);
    }

    /**
     * Runs the install unless {@code package.json} and the lock file are unchanged since the last install and
     * {@code node_modules} still exists. For build tools without up-to-date checks of their own (Maven).
     *
     * @param packageManager {@code auto}, {@code npm}, {@code pnpm} or {@code yarn}
     * @return whether the install ran
     * @throws IOException if the install fails
     */
    public boolean installIfChanged(String packageManager) throws IOException {
        requirePackageJson();
        Path stamp = frontendDir.resolve(INSTALL_STAMP);
        String hash = packageFilesHash(packageManager);
        if (Files.isRegularFile(stamp) && hash.equals(new String(Files.readAllBytes(stamp), StandardCharsets.UTF_8))) {
            log.info("mustache-react: frontend dependencies are up to date");
            return false;
        }
        install(packageManager);
        // The install may have written the lock file: hash again.
        Files.createDirectories(stamp.getParent());
        Files.write(stamp, packageFilesHash(packageManager).getBytes(StandardCharsets.UTF_8));
        return true;
    }

    /** Records the inputs of the last install, inside node_modules so that deleting node_modules forces an install. */
    static final String INSTALL_STAMP = "node_modules/.mustache-react-install";

    private String packageFilesHash(String packageManager) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
        digest.update(packageManager.getBytes(StandardCharsets.UTF_8));
        for (String name : PackageManager.inputFiles()) {
            Path file = frontendDir.resolve(name);
            digest.update(name.getBytes(StandardCharsets.UTF_8));
            if (Files.isRegularFile(file)) {
                digest.update(Files.readAllBytes(file));
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Builds the frontend.
     *
     * @param outDir directory Spring serves at the public path, e.g. {@code build/mustache-react/static/mustache-react}
     * @param base public path, e.g. {@code /mustache-react/}
     * @throws IOException if the CLI is missing or the build fails
     */
    public void build(Path outDir, String base) throws IOException {
        build(outDir, base, null);
    }

    /**
     * Builds the frontend and, when {@code ssrOutDir} is given, the server-side rendering bundle
     * ({@code server.mjs}).
     *
     * @param outDir directory Spring serves at the public path
     * @param base public path, e.g. {@code /mustache-react/}
     * @param ssrOutDir where to write {@code server.mjs}, e.g. {@code <resources>/META-INF/mustache-react/ssr};
     *        {@code null} to skip it
     * @throws IOException if the CLI is missing or the build fails
     */
    public void build(Path outDir, String base, Path ssrOutDir) throws IOException {
        requirePackageJson();
        Path cli = frontendDir.resolve(CLI);
        if (!Files.isRegularFile(cli)) {
            throw new IOException("mustache-react: " + cli + " not found. Add @mustache-react/vite-plugin to the "
                    + "devDependencies of " + frontendDir.resolve("package.json") + " and install the dependencies.");
        }
        log.info("mustache-react: building the frontend into " + outDir
                + (ssrOutDir == null ? "" : ", and the SSR bundle into " + ssrOutDir));
        List<String> command = node.node(cli.toString(), "build", "--out-dir", outDir.toString(), "--base", base);
        if (ssrOutDir != null) {
            command.add("--ssr");
            command.add("--ssr-out-dir");
            command.add(ssrOutDir.toString());
        }
        ProcessRunner.run(command, frontendDir, environment(), log);
    }

    private void requirePackageJson() throws IOException {
        // A new project gets package.json, tsconfig.json and .gitignore from the plugin.
        FrontendScaffold.createIfMissing(frontendDir, log);
        if (!Files.isRegularFile(frontendDir.resolve("package.json"))) {
            throw new IOException("mustache-react: no package.json in " + frontendDir
                    + ". Create the frontend there (see the mustache-react README).");
        }
    }

    private Map<String, String> environment() {
        Map<String, String> environment = new HashMap<>(System.getenv());
        node.applyTo(environment);
        return environment;
    }
}
