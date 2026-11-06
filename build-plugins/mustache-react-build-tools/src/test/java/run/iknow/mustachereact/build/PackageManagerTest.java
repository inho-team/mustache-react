package run.iknow.mustachereact.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageManagerTest {

    @TempDir
    Path dir;

    private final NodeRuntime node = NodeRuntime.installed(java.nio.file.Paths.get("/n"), Platform.of("Linux", "amd64"));

    private void write(String name, String content) throws IOException {
        Files.write(dir.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void defaultsToNpmAndUsesCiWithALockFile() throws IOException {
        write("package.json", "{}");
        assertThat(PackageManager.detect(dir, "auto").installCommand(node))
                .containsExactly("/n/bin/node", "/n/lib/node_modules/npm/bin/npm-cli.js", "install");

        write("package-lock.json", "{}");
        assertThat(PackageManager.detect(dir, "auto").installCommand(node))
                .containsExactly("/n/bin/node", "/n/lib/node_modules/npm/bin/npm-cli.js", "ci");
    }

    @Test
    void runsTheDeclaredPackageManagerVersionThroughNpmExec() throws IOException {
        write("package.json", "{\"name\":\"x\",\"packageManager\": \"pnpm@10.28.0+sha512.abc\"}");
        write("pnpm-lock.yaml", "");

        PackageManager manager = PackageManager.detect(dir, "auto");

        assertThat(manager.toString()).isEqualTo("pnpm@10.28.0");
        assertThat(manager.installCommand(node)).containsExactly("/n/bin/node", "/n/lib/node_modules/npm/bin/npm-cli.js",
                "exec", "--yes", "--", "pnpm@10.28.0", "install", "--frozen-lockfile");
    }

    @Test
    void detectsFromTheLockFileOrTheConfiguration() throws IOException {
        write("package.json", "{}");
        write("yarn.lock", "");

        assertThat(PackageManager.detect(dir, "auto").kind()).isEqualTo(PackageManager.Kind.YARN);
        assertThat(PackageManager.detect(dir, "auto").installCommand(node)).endsWith("install", "--frozen-lockfile");
        assertThat(PackageManager.detect(dir, "npm").kind()).isEqualTo(PackageManager.Kind.NPM);
        assertThat(PackageManager.detect(dir, "pnpm").installCommand(node)).containsExactly("pnpm", "install");
    }
}
