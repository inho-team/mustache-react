package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import run.iknow.mustachereact.core.MustacheReactVersion;

class ViteManifestSourceTest {

    @TempDir
    Path dir;

    @Test
    void notesAFrontendBuiltWithAnotherVitePluginVersion() {
        ViteManifestSource source = new ViteManifestSource(new DefaultResourceLoader(),
                "classpath:/version/old/.vite/manifest.json");

        source.get();

        assertThat(source.getVersionMismatch()).isEqualTo("0.0.1");
    }

    @Test
    void acceptsTheSameVersionOrNoBuildInfo() throws Exception {
        Files.createDirectories(dir.resolve(".vite"));
        Files.copy(getClass().getResourceAsStream("/renderer/manifest.json"), dir.resolve(".vite/manifest.json"));
        Files.write(dir.resolve(ViteManifestSource.BUILD_INFO),
                ("{\"version\":\"" + MustacheReactVersion.get() + "\"}").getBytes(StandardCharsets.UTF_8));
        ViteManifestSource same = new ViteManifestSource(new DefaultResourceLoader(),
                dir.resolve(".vite/manifest.json").toUri().toString());
        ViteManifestSource none = new ViteManifestSource(new DefaultResourceLoader(),
                "classpath:/version/same/.vite/manifest.json");

        same.get();
        none.get();

        assertThat(same.getVersionMismatch()).isNull();
        assertThat(none.getVersionMismatch()).isNull();
    }
}
