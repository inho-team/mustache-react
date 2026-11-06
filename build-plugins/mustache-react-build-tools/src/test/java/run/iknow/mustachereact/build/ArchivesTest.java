package run.iknow.mustachereact.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchivesTest {

    @TempDir
    Path dir;

    private static String read(Path file) throws Exception {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test
    void extractsFilesLongNamesSymlinksAndExecutableBits() throws Exception {
        assumeFalse(Platform.current().isWindows());
        String longName = "node/lib/node_modules/npm/node_modules/" + new String(new char[80]).replace('\0', 'a') + "/index.js";
        Path archive = new TarFixtures()
                .file("node/bin/node", "#!/bin/sh\necho node\n", 0755)
                .longFile(longName, "long")
                .symlink("node/bin/npm", "../lib/npm-cli.js")
                .writeTo(dir.resolve("node.tar.gz"));

        Path target = dir.resolve("out");
        Archives.extract(archive, target);

        assertThat(read(target.resolve("node/bin/node"))).startsWith("#!/bin/sh");
        assertThat(Files.isExecutable(target.resolve("node/bin/node"))).isTrue();
        assertThat(read(target.resolve(longName))).isEqualTo("long");
        assertThat(Files.isSymbolicLink(target.resolve("node/bin/npm"))).isTrue();
        assertThat(Files.readSymbolicLink(target.resolve("node/bin/npm")).toString()).isEqualTo("../lib/npm-cli.js");
    }

    @Test
    void rejectsEntriesOutsideTheTarget() throws Exception {
        Path escaping = new TarFixtures().file("../evil.txt", "x", 0644).writeTo(dir.resolve("a.tar.gz"));
        Path linking = new TarFixtures().symlink("node/link", "../../../etc/passwd").writeTo(dir.resolve("b.tar.gz"));

        assertThatThrownBy(() -> Archives.extract(escaping, dir.resolve("out1"))).hasMessageContaining("outside the target");
        assertThatThrownBy(() -> Archives.extract(linking, dir.resolve("out2"))).hasMessageContaining("outside the target");
        assertThat(dir.resolve("evil.txt")).doesNotExist();
    }
}
