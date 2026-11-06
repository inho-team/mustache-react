package run.iknow.mustachereact.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlatformTest {

    @Test
    void namesNodeDistributions() {
        assertThat(Platform.of("Linux", "amd64").archiveName("24.22.2")).isEqualTo("node-v24.22.2-linux-x64.tar.gz");
        assertThat(Platform.of("Mac OS X", "aarch64").archiveName("24.22.2")).isEqualTo("node-v24.22.2-darwin-arm64.tar.gz");
        assertThat(Platform.of("Windows 11", "amd64").archiveName("24.22.2")).isEqualTo("node-v24.22.2-win-x64.zip");
    }

    @Test
    void explainsUnsupportedPlatforms() {
        assertThatThrownBy(() -> Platform.of("SunOS", "sparcv9")).hasMessageContaining("turn the download off");
        assertThatThrownBy(() -> Platform.of("Linux", "s390x")).hasMessageContaining("CPU 's390x'");
    }
}
