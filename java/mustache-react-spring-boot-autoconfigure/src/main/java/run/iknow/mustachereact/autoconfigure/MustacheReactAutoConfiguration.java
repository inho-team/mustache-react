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

import com.samskivert.mustache.Mustache;

import run.iknow.mustachereact.spring.ReactPageHandlerAdapterPostProcessor;
import run.iknow.mustachereact.spring.ReactPageRenderer;

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
    public ReactPageRenderer reactPageRenderer(ResourceLoader resourceLoader, MustacheReactProperties properties,
            ObjectProvider<Mustache.Compiler> compiler) {
        return new ReactPageRenderer(resourceLoader, compiler.getIfAvailable(Mustache::compiler),
                properties.getManifestLocation(), properties.getLayout(), properties.getPublicPath(),
                properties.isContentNegotiation());
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
