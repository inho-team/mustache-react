package run.iknow.mustachereact.build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Creates the frontend project files when there is none yet, so a Spring application can start with only the build
 * plugin: {@code package.json} (React, Vite, the mustache-react packages of the plugin's version),
 * {@code tsconfig.json}, {@code .gitignore}, and empty {@code pages/} and {@code islands/} directories.
 * Existing files are never changed.
 */
public final class FrontendScaffold {

    private static final String RESOURCES = "scaffold/";

    private FrontendScaffold() {
    }

    /**
     * @return the version of the mustache-react build tools, which the generated {@code package.json} uses for
     *     the {@code @mustache-react/*} packages
     */
    public static String version() {
        Properties properties = new Properties();
        try (InputStream in = FrontendScaffold.class.getResourceAsStream("mustache-react-build.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the mustache-react version", e);
        }
        return properties.getProperty("version", "latest");
    }

    /**
     * Creates the frontend project if {@code frontendDir} has no {@code package.json}.
     *
     * @param frontendDir e.g. {@code src/main/frontend}
     * @param log where to report the created files
     * @return the files created; empty when the project already existed
     * @throws IOException if writing fails
     */
    public static List<Path> createIfMissing(Path frontendDir, BuildLog log) throws IOException {
        List<Path> created = new ArrayList<>();
        if (Files.isRegularFile(frontendDir.resolve("package.json"))) {
            return created;
        }
        Files.createDirectories(frontendDir);
        write(frontendDir.resolve("package.json"), template("package.json").replace("@VERSION@", version()), created);
        write(frontendDir.resolve("tsconfig.json"), template("tsconfig.json"), created);
        write(frontendDir.resolve(".gitignore"), template("gitignore"), created);
        for (String dir : new String[] {"pages", "islands"}) {
            Path path = frontendDir.resolve(dir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                created.add(path);
            }
        }
        log.info("mustache-react: created a frontend project in " + frontendDir + " (" + created.size()
                + " files and directories). Commit them; put React pages in pages/ and islands in islands/.");
        return created;
    }

    private static void write(Path file, String content, List<Path> created) throws IOException {
        if (!Files.exists(file)) {
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            created.add(file);
        }
    }

    private static String template(String name) throws IOException {
        try (InputStream in = FrontendScaffold.class.getResourceAsStream(RESOURCES + name)) {
            if (in == null) {
                throw new IOException("mustache-react: missing template " + name);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
