package run.iknow.mustachereact.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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
@ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MustacheReactProperties.class)
public class MustacheReactAutoConfiguration {
}
