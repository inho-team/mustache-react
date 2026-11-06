package run.iknow.mustachereact.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A Node.js installation: either one downloaded by {@link NodeInstaller}, or the {@code node} found on the
 * {@code PATH}.
 */
public final class NodeRuntime {

    private final Path home;
    private final Path node;
    private final Path npmCli;

    private NodeRuntime(Path home, Path node, Path npmCli) {
        this.home = home;
        this.node = node;
        this.npmCli = npmCli;
    }

    static NodeRuntime installed(Path home, Platform platform) {
        if (platform.isWindows()) {
            return new NodeRuntime(home, home.resolve("node.exe"), home.resolve("node_modules/npm/bin/npm-cli.js"));
        }
        return new NodeRuntime(home, home.resolve("bin/node"), home.resolve("lib/node_modules/npm/bin/npm-cli.js"));
    }

    /**
     * @return the {@code node} and {@code npm} commands on the {@code PATH}
     */
    public static NodeRuntime fromPath() {
        return new NodeRuntime(null, null, null);
    }

    /**
     * @return the installation directory, or {@code null} when using the {@code PATH}
     */
    public Path home() {
        return home;
    }

    /**
     * @return the node executable, or {@code null} when using the {@code PATH}
     */
    public Path node() {
        return node;
    }

    /**
     * @return the command that runs {@code node}
     */
    public String nodeCommand() {
        return node == null ? "node" : node.toString();
    }

    /**
     * @param args arguments
     * @return a command line running node with the arguments
     */
    public List<String> node(String... args) {
        List<String> command = new ArrayList<>();
        command.add(nodeCommand());
        command.addAll(Arrays.asList(args));
        return command;
    }

    /**
     * @param args arguments
     * @return a command line running npm with the arguments
     */
    public List<String> npm(String... args) {
        List<String> command = new ArrayList<>();
        if (npmCli == null) {
            command.add(Platform.current().isWindows() ? "npm.cmd" : "npm");
        } else {
            command.add(node.toString());
            command.add(npmCli.toString());
        }
        command.addAll(Arrays.asList(args));
        return command;
    }

    /**
     * Puts this installation first on the {@code PATH} of a child process, so that scripts run by the package manager
     * ({@code postinstall}, {@code node} shebangs) use it as well.
     *
     * @param environment the child process environment, modified in place
     */
    public void applyTo(Map<String, String> environment) {
        if (node == null) {
            return;
        }
        String key = "PATH";
        for (String name : environment.keySet()) {
            if (name.equalsIgnoreCase("PATH")) {
                key = name;
            }
        }
        String existing = environment.get(key);
        String bin = node.getParent().toString();
        environment.put(key, existing == null || existing.isEmpty() ? bin : bin + File.pathSeparator + existing);
    }
}
