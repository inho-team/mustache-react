package run.iknow.mustachereact.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FrontendScaffoldTest {

    @TempDir
    Path dir;

    private final List<String> messages = new ArrayList<>();
    private final BuildLog log = new BuildLog() {
        @Override
        public void info(String message) {
            messages.add(message);
        }

        @Override
        public void warn(String message) {
            messages.add(message);
        }
    };

    private String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test
    void createsTheProjectWithThePluginsVersion() throws IOException {
        Path frontend = dir.resolve("src/main/frontend");

        List<Path> created = FrontendScaffold.createIfMissing(frontend, log);

        assertThat(created).hasSize(5);
        assertThat(read(frontend.resolve("package.json")))
                .contains("\"@mustache-react/client\": \"" + FrontendScaffold.version() + "\"")
                .contains("\"@mustache-react/vite-plugin\": \"" + FrontendScaffold.version() + "\"")
                .doesNotContain("@VERSION@");
        assertThat(FrontendScaffold.version()).matches("\\d+\\.\\d+\\.\\d+.*");
        assertThat(read(frontend.resolve("tsconfig.json"))).contains("\"jsx\": \"react-jsx\"");
        assertThat(read(frontend.resolve(".gitignore"))).isEqualTo("node_modules/\n");
        assertThat(frontend.resolve("pages")).isDirectory();
        assertThat(frontend.resolve("islands")).isDirectory();
        assertThat(messages).singleElement().asString().contains("created a frontend project");
    }

    @Test
    void leavesAnExistingProjectAlone() throws IOException {
        Files.write(dir.resolve("package.json"), "{}".getBytes(StandardCharsets.UTF_8));

        assertThat(FrontendScaffold.createIfMissing(dir, log)).isEmpty();
        assertThat(dir.resolve("tsconfig.json")).doesNotExist();
        assertThat(messages).isEmpty();
    }

    /** The template follows the consumer example, which CI builds against the real packages. */
    @Test
    void usesTheSameDependencyVersionsAsTheConsumerExample() throws IOException {
        Path consumer = Paths.get("../../examples/consumer/src/main/frontend/package.json");
        Path template = Paths.get("src/main/resources/run/iknow/mustachereact/build/scaffold/package.json");
        assertThat(versions(read(template))).isEqualTo(versions(read(consumer)));
    }

    private static List<String> versions(String packageJson) {
        List<String> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"((?:@types/|@vitejs/)?[a-z-]+)\": \"(\\^[^\"]+)\"").matcher(packageJson);
        while (matcher.find()) {
            result.add(matcher.group(1) + "=" + matcher.group(2));
        }
        return result;
    }
}
