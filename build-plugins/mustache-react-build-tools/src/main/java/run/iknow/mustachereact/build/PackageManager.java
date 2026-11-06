package run.iknow.mustachereact.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The package manager of a frontend directory, from the {@code packageManager} field of {@code package.json}
 * (for example {@code "pnpm@10.28.0"}) or its lock file. npm is the default.
 */
public final class PackageManager {

    private static final Pattern PACKAGE_MANAGER_FIELD =
            Pattern.compile("\"packageManager\"\\s*:\\s*\"(npm|pnpm|yarn)@([^\"+]+)[^\"]*\"");

    /** Kinds of package managers. */
    public enum Kind {
        NPM, PNPM, YARN
    }

    private final Kind kind;
    private final String version;
    private final boolean locked;

    PackageManager(Kind kind, String version, boolean locked) {
        this.kind = kind;
        this.version = version;
        this.locked = locked;
    }

    /**
     * @param frontendDir directory holding {@code package.json}
     * @param configured {@code auto}, {@code npm}, {@code pnpm} or {@code yarn}
     * @return the package manager
     * @throws IOException if {@code package.json} cannot be read
     */
    public static PackageManager detect(Path frontendDir, String configured) throws IOException {
        Path packageJson = frontendDir.resolve("package.json");
        String json = Files.isRegularFile(packageJson)
                ? new String(Files.readAllBytes(packageJson), StandardCharsets.UTF_8) : "";
        Matcher field = PACKAGE_MANAGER_FIELD.matcher(json);
        Kind declared = null;
        String version = null;
        if (field.find()) {
            declared = Kind.valueOf(field.group(1).toUpperCase(Locale.ROOT));
            version = field.group(2);
        }

        Kind kind;
        if (configured != null && !configured.isEmpty() && !"auto".equalsIgnoreCase(configured)) {
            kind = Kind.valueOf(configured.toUpperCase(Locale.ROOT));
        } else if (declared != null) {
            kind = declared;
        } else if (Files.exists(frontendDir.resolve("pnpm-lock.yaml"))) {
            kind = Kind.PNPM;
        } else if (Files.exists(frontendDir.resolve("yarn.lock"))) {
            kind = Kind.YARN;
        } else {
            kind = Kind.NPM;
        }
        return new PackageManager(kind, kind == declared ? version : null, Files.exists(frontendDir.resolve(lockFile(kind))));
    }

    static String lockFile(Kind kind) {
        switch (kind) {
            case PNPM:
                return "pnpm-lock.yaml";
            case YARN:
                return "yarn.lock";
            default:
                return "package-lock.json";
        }
    }

    /** @return the kind */
    public Kind kind() {
        return kind;
    }

    /** @return files whose change requires a new install, relative to the frontend directory */
    public static List<String> inputFiles() {
        return Arrays.asList("package.json", "package-lock.json", "pnpm-lock.yaml", "yarn.lock");
    }

    /**
     * The install command. With a lock file it installs exactly what the lock file says ({@code npm ci},
     * {@code --frozen-lockfile}), as a CI build should. pnpm and yarn run in the version named by
     * {@code packageManager} through {@code npm exec}, or from the {@code PATH} when no version is named.
     *
     * @param node the Node.js installation
     * @return the command line
     */
    public List<String> installCommand(NodeRuntime node) {
        switch (kind) {
            case PNPM:
                return tool(node, "pnpm", locked ? new String[] {"install", "--frozen-lockfile"} : new String[] {"install"});
            case YARN:
                return tool(node, "yarn", locked ? new String[] {"install", "--frozen-lockfile"} : new String[] {"install"});
            default:
                return node.npm(locked ? new String[] {"ci"} : new String[] {"install"});
        }
    }

    private List<String> tool(NodeRuntime node, String name, String[] args) {
        if (version == null) {
            List<String> command = new ArrayList<>();
            command.add(Platform.current().isWindows() ? name + ".cmd" : name);
            command.addAll(Arrays.asList(args));
            return command;
        }
        List<String> command = node.npm("exec", "--yes", "--", name + "@" + version);
        command.addAll(Arrays.asList(args));
        return command;
    }

    @Override
    public String toString() {
        return kind.name().toLowerCase(Locale.ROOT) + (version == null ? "" : "@" + version);
    }
}
