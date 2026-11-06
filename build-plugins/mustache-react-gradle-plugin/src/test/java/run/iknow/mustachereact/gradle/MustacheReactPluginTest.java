package run.iknow.mustachereact.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs real Gradle builds with the plugin. The frontend CLI is replaced by a script that writes a manifest, so the
 * tests check the Gradle wiring without installing npm packages.
 */
class MustacheReactPluginTest {

    @TempDir
    Path project;

    private static final String FAKE_CLI = ""
            + "const fs = require('fs'), path = require('path');\n"
            + "const args = process.argv.slice(2);\n"
            + "const out = args[args.indexOf('--out-dir') + 1];\n"
            + "fs.mkdirSync(path.join(out, '.vite'), { recursive: true });\n"
            + "fs.mkdirSync(path.join(out, 'assets'), { recursive: true });\n"
            + "fs.writeFileSync(path.join(out, 'assets', 'entry.js'), 'console.log(1)');\n"
            + "fs.writeFileSync(path.join(out, '.vite', 'manifest.json'), JSON.stringify({ 'virtual:mustache-react/entry':"
            + " { file: 'assets/entry.js', name: 'mustache-react', isEntry: true } }));\n"
            + "fs.appendFileSync(path.join(process.cwd(), '..', '..', '..', 'cli-calls.txt'), args.join(' ') + '\\n');\n";

    private void write(String path, String content) throws IOException {
        Path file = project.resolve(path);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private String read(String path) throws IOException {
        return new String(Files.readAllBytes(project.resolve(path)), StandardCharsets.UTF_8);
    }

    @BeforeEach
    void createProject() throws IOException {
        write("settings.gradle", "rootProject.name = 'app'\n");
        write("build.gradle", ""
                + "plugins {\n"
                + "  id 'java'\n"
                + "  id 'run.iknow.mustache-react'\n"
                + "}\n"
                + "mustacheReact {\n"
                + "  nodeDownload = false\n"
                + "  install = false\n"
                + "  types = false\n"
                + "}\n"
                + "tasks.register('printWiring') {\n"
                + "  def args = tasks.named('compileJava').map { it.options.allCompilerArgs }\n"
                + "  def processors = provider { configurations.annotationProcessor.dependencies.collect { \"${it.group}:${it.name}:${it.version}\" } }\n"
                + "  doLast { println \"ARGS=${args.get()}\"; println \"PROCESSORS=${processors.get()}\" }\n"
                + "}\n");
        write("src/main/java/app/App.java", "package app; public class App { }\n");
        write("src/main/frontend/package.json", "{ \"name\": \"frontend\", \"private\": true }\n");
        write("src/main/frontend/pages/Home.tsx", "export default function Home() { return null; }\n");
        write("src/main/frontend/node_modules/@mustache-react/vite-plugin/dist/bin.js", FAKE_CLI);
    }

    private GradleRunner runner(String... args) {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments.add("--stacktrace");
        return GradleRunner.create().withProjectDir(project.toFile()).withPluginClasspath().withArguments(arguments)
                .forwardOutput();
    }

    @Test
    void buildsTheFrontendIntoTheJar() throws IOException {
        BuildResult result = runner("jar").build();

        assertThat(result.task(":mustacheReactBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.task(":mustacheReactInstall").getOutcome()).isEqualTo(TaskOutcome.SKIPPED);
        File jar = project.resolve("build/libs/app.jar").toFile();
        try (ZipFile zip = new ZipFile(jar)) {
            assertThat(zip.getEntry("static/mustache-react/.vite/manifest.json")).isNotNull();
            assertThat(zip.getEntry("static/mustache-react/assets/entry.js")).isNotNull();
        }
        assertThat(read("cli-calls.txt")).isEqualTo("build --out-dir "
                + project.resolve("build/mustache-react/resources/static/mustache-react").toFile().getAbsolutePath()
                + " --base /mustache-react/\n");
    }

    @Test
    void passesTheSsrOutputDirectoryWhenSsrIsOn() throws IOException {
        write("build.gradle", read("build.gradle").replace("types = false", "types = false\n  ssr = true"));

        runner("processResources").build();

        assertThat(read("cli-calls.txt")).contains(" --ssr --ssr-out-dir "
                + project.resolve("build/mustache-react/resources/META-INF/mustache-react/ssr").toFile().getAbsolutePath());
    }

    @Test
    void isUpToDateUntilAFrontendSourceChanges() throws IOException {
        runner("processResources").build();

        assertThat(runner("processResources").build().task(":mustacheReactBuild").getOutcome())
                .isEqualTo(TaskOutcome.UP_TO_DATE);

        write("src/main/frontend/pages/Home.tsx", "export default function Home() { return 'changed'; }\n");
        assertThat(runner("processResources").build().task(":mustacheReactBuild").getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    void canSkipTheFrontendBuild() {
        BuildResult result = runner("processResources", "-PmustacheReact.skipFrontend").build();

        assertThat(result.task(":mustacheReactBuild").getOutcome()).isEqualTo(TaskOutcome.SKIPPED);
        assertThat(project.resolve("build/resources/main/static")).doesNotExist();
    }

    @Test
    void wiresTheTypeGenerator() throws IOException {
        write("build.gradle", read("build.gradle").replace("types = false", "types = true"));
        String output = runner("printWiring").build().getOutput();

        assertThat(output)
                .contains("-Amustachereact.typesFile=" + project.resolve("src/main/frontend/generated/types.ts").toFile().getAbsolutePath())
                .contains("PROCESSORS=[run.iknow:mustache-react-processor:" + MustacheReactPlugin.pluginVersion() + "]");

        write("build.gradle", read("build.gradle").replace("types = true", "types = false"));
        String disabled = runner("printWiring").build().getOutput();
        assertThat(disabled).doesNotContain("-Amustachereact.typesFile").contains("PROCESSORS=[]");
    }

    @Test
    void passesTheFrontendDirectoryToTheApplication() throws IOException {
        write("build.gradle", read("build.gradle") + "\napply plugin: 'application'\napplication { mainClass = 'app.App' }\n"
                + "tasks.register('printRunArgs') { doLast { println \"RUN=${tasks.run.allJvmArgs}\" } }\n");

        String output = runner("printRunArgs").build().getOutput();

        assertThat(output).contains("-Dmustache-react.frontend-dir=" + project.resolve("src/main/frontend").toFile().getAbsolutePath());
    }

    @Test
    void installsWithThePackageManagerAndStaysUpToDate() throws IOException {
        write("build.gradle", read("build.gradle").replace("install = false", "install = true"));

        BuildResult first = runner("mustacheReactInstall").build();
        assertThat(first.task(":mustacheReactInstall").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(first.getOutput()).contains("installing frontend dependencies with npm");
        assertThat(project.resolve("src/main/frontend/package-lock.json")).exists();

        // package-lock.json now exists: the next install is "npm ci", once, then up to date.
        assertThat(runner("mustacheReactInstall").build().task(":mustacheReactInstall").getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);
        assertThat(runner("mustacheReactInstall").build().task(":mustacheReactInstall").getOutcome())
                .isEqualTo(TaskOutcome.UP_TO_DATE);
    }

    @Test
    void worksWithTheConfigurationCache() {
        runner("processResources", "--configuration-cache").build();
        BuildResult reused = runner("processResources", "--configuration-cache").build();

        assertThat(reused.getOutput()).contains("Reusing configuration cache");
    }

    @Test
    void worksOnGradle7() throws IOException {
        String javaHome = System.getProperty("mustacheReact.gradle7JavaHome");
        assumeTrue(javaHome != null && !javaHome.isEmpty(), "set -PtestGradle7JavaHome to a JDK 8-17");
        write("gradle.properties", "org.gradle.java.home=" + javaHome.replace("\\", "\\\\") + "\n");

        BuildResult result = runner("jar").withGradleVersion("7.6.4").build();

        assertThat(result.task(":mustacheReactBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(project.resolve("build/resources/main/static/mustache-react/.vite/manifest.json")).exists();
    }
}
