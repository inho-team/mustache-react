package run.iknow.mustachereact.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import run.iknow.mustachereact.spring.ReactPageHandlerAdapterPostProcessor;
import run.iknow.mustachereact.spring.ReactPageRenderer;

class MustacheReactAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MustacheReactAutoConfiguration.class));

    @Test
    void bindsDefaults() {
        runner.run(context -> {
            MustacheReactProperties properties = context.getBean(MustacheReactProperties.class);
            assertThat(properties.getSsr()).isEqualTo(MustacheReactProperties.SsrMode.OFF);
            assertThat(properties.getFrontendDir()).isEqualTo("src/main/frontend");
            assertThat(properties.isContentNegotiation()).isTrue();
            assertThat(properties.getManifestLocation()).isEqualTo("classpath:/static/mustache-react/.vite/manifest.json");
            assertThat(properties.getPublicPath()).isEqualTo("/mustache-react/");
            assertThat(context).hasSingleBean(ReactPageRenderer.class);
            assertThat(context).hasSingleBean(ReactPageHandlerAdapterPostProcessor.class);
        });
    }

    @Test
    void bindsApplicationProperties() {
        runner.withPropertyValues("mustache-react.ssr=auto", "mustache-react.content-negotiation=false")
                .run(context -> {
                    MustacheReactProperties properties = context.getBean(MustacheReactProperties.class);
                    assertThat(properties.getSsr()).isEqualTo(MustacheReactProperties.SsrMode.AUTO);
                    assertThat(properties.isContentNegotiation()).isFalse();
                    assertThat(context.getBean(ReactPageRenderer.class).isContentNegotiation()).isFalse();
                });
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("mustache-react.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MustacheReactProperties.class);
                    assertThat(context).doesNotHaveBean(ReactPageRenderer.class);
                });
    }

    @Test
    void backsOffOutsideWebApplications() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MustacheReactAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(MustacheReactProperties.class));
    }
}
