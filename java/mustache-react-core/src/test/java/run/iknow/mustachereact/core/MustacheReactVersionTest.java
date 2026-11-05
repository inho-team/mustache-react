package run.iknow.mustachereact.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MustacheReactVersionTest {

    @Test
    void versionIsFilledInByTheBuild() {
        assertThat(MustacheReactVersion.get())
                .isNotEqualTo("unknown")
                .doesNotContain("${");
    }
}
