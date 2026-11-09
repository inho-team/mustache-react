package run.iknow.mustachereact.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FrontendBuildTest {

    @TempDir
    Path dir;

    private static boolean npmAvailable() {
        try {
            return new ProcessBuilder(NodeRuntime.fromPath().npm("--version")).start().waitFor() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    void installsOnlyWhenThePackageFilesChange() throws Exception {
        assumeTrue(npmAvailable(), "needs npm on the PATH");
        Files.write(dir.resolve("package.json"), "{\"name\":\"x\",\"private\":true}".getBytes(StandardCharsets.UTF_8));
        FrontendBuild build = new FrontendBuild(dir, NodeRuntime.fromPath(), BuildLog.STANDARD);

        assertThat(build.installIfChanged("auto")).isTrue();
        assertThat(build.installIfChanged("auto")).isFalse();

        Files.write(dir.resolve("package.json"), "{\"name\":\"y\",\"private\":true}".getBytes(StandardCharsets.UTF_8));
        assertThat(build.installIfChanged("auto")).isTrue();

        NodeInstaller.deleteRecursively(dir.resolve("node_modules"));
        assertThat(build.installIfChanged("auto")).isTrue();
    }

    @Test
    void createsAMissingProjectAndExplainsAMissingCli() throws Exception {
        FrontendBuild build = new FrontendBuild(dir, NodeRuntime.fromPath(), BuildLog.STANDARD);

        // No package.json: the project is created, then the dependencies are still to be installed.
        assertThatThrownBy(() -> build.build(dir.resolve("out"), "/mustache-react/"))
                .hasMessageContaining("install the dependencies");
        assertThat(dir.resolve("package.json")).isRegularFile();
        assertThatThrownBy(() -> build.build(dir.resolve("out"), "/mustache-react/"))
                .hasMessageContaining("Add @mustache-react/vite-plugin to the devDependencies");
    }
}
