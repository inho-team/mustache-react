package run.iknow.mustachereact.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.samskivert.mustache.Mustache;

import run.iknow.mustachereact.spring.MustacheReactHelpers;
import run.iknow.mustachereact.spring.MustacheReactModelAdvice;
import run.iknow.mustachereact.spring.PropsSerializer;
import run.iknow.mustachereact.spring.ReactIslandLambda;
import run.iknow.mustachereact.spring.ReactPageHandlerAdapterPostProcessor;
import run.iknow.mustachereact.spring.ReactPageRenderer;
import run.iknow.mustachereact.spring.ViteManifestSource;

/**
 * Auto-configuration for mustache-react.
 *
 * <p>Registered through {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * which Spring Boot reads from 2.7 onward. Ordering against other auto-configurations uses class names as strings,
 * because Spring Boot 4 moved the Mustache auto-configuration to a different package.
 */
@AutoConfiguration(afterName = {
        // Spring Boot 2.7, 3.x
        "org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration",
        // Spring Boot 4.x
        "org.springframework.boot.mustache.autoconfigure.MustacheAutoConfiguration",
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = {
        "com.samskivert.mustache.Mustache",
        "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter",
})
@ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MustacheReactProperties.class)
public class MustacheReactAutoConfiguration {

    /**
     * Renders {@code @ReactPage} pages. Uses the application's Mustache compiler when there is one, so a custom
     * layout can include the application's partials.
     */
    @Bean
    @ConditionalOnMissingBean
    public ViteManifestSource mustacheReactManifest(ResourceLoader resourceLoader, MustacheReactProperties properties) {
        return new ViteManifestSource(resourceLoader, properties.getManifestLocation());
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactPageRenderer reactPageRenderer(ResourceLoader resourceLoader, MustacheReactProperties properties,
            ObjectProvider<Mustache.Compiler> compiler, ViteManifestSource manifest) {
        return new ReactPageRenderer(resourceLoader, compiler.getIfAvailable(Mustache::compiler), manifest,
                properties.getLayout(), properties.getPublicPath(), properties.isContentNegotiation());
    }

    /**
     * Serializes island props with Spring MVC's message converters, looked up on first use.
     */
    @Bean
    @ConditionalOnMissingBean
    public PropsSerializer mustacheReactPropsSerializer(ObjectProvider<RequestMappingHandlerAdapter> adapter) {
        return new PropsSerializer(() -> adapter.getObject().getMessageConverters());
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactIslandLambda reactIslandLambda(PropsSerializer propsSerializer) {
        return new ReactIslandLambda(propsSerializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public MustacheReactHelpers mustacheReactHelpers(ViteManifestSource manifest, MustacheReactProperties properties) {
        return new MustacheReactHelpers(manifest, properties.getPublicPath());
    }

    /**
     * Makes {@code {{#react}}}, {@code {{{mr.head}}}} and {@code {{{mr.scripts}}}} available in every Mustache view.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "template.model-attributes", matchIfMissing = true)
    public MustacheReactModelAdvice mustacheReactModelAdvice(MustacheReactProperties properties,
            ReactIslandLambda react, MustacheReactHelpers helpers) {
        return new MustacheReactModelAdvice(properties.getTemplate().getReactName(), react,
                properties.getTemplate().getHelpersName(), helpers);
    }

    /**
     * Installs the {@code @ReactPage} return value handler. Static, as bean post-processors must be created before
     * regular beans.
     */
    @Bean
    public static ReactPageHandlerAdapterPostProcessor reactPageHandlerAdapterPostProcessor(
            ObjectProvider<ReactPageRenderer> renderer) {
        return new ReactPageHandlerAdapterPostProcessor(renderer);
    }
}
