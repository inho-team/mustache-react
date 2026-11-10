package run.iknow.mustachereact.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import run.iknow.mustachereact.spring.YamlMessageSource;

/**
 * Messages in YAML: {@code messages.yml}, {@code messages_en.yml}, ... (base names from
 * {@code spring.messages.basename}). With only YAML files they are the application's message source; next to
 * {@code messages.properties} they back it up (keys missing from the properties files are looked up in YAML).
 * Runs before Spring Boot's message source auto-configuration.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration")
@ConditionalOnClass(name = "org.yaml.snakeyaml.Yaml")
@ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "enabled", matchIfMissing = true)
public class MustacheReactMessagesAutoConfiguration {

    /** The message source when there are YAML bundles and no {@code messages.properties}. */
    @Bean(name = "messageSource")
    @ConditionalOnMissingBean(name = "messageSource")
    @Conditional(OnlyYamlBundles.class)
    public YamlMessageSource messageSource(Environment environment, ResourceLoader resourceLoader) {
        return create(environment, resourceLoader);
    }

    /** With properties and YAML bundles: YAML answers what the properties files do not define. */
    @Bean
    static BeanPostProcessor mustacheReactYamlMessages(Environment environment, ResourceLoader resourceLoader) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if ("messageSource".equals(beanName) && bean instanceof AbstractMessageSource
                        && !(bean instanceof YamlMessageSource)
                        && ((AbstractMessageSource) bean).getParentMessageSource() == null
                        && YamlMessageSource.exists(resolver(resourceLoader), basenames(environment))) {
                    ((AbstractMessageSource) bean).setParentMessageSource(create(environment, resourceLoader));
                }
                return bean;
            }
        };
    }

    static YamlMessageSource create(Environment environment, ResourceLoader resourceLoader) {
        YamlMessageSource source = new YamlMessageSource(resolver(resourceLoader), basenames(environment));
        Binder binder = Binder.get(environment);
        source.setAlwaysUseMessageFormat(
                binder.bind("spring.messages.always-use-message-format", Boolean.class).orElse(false));
        source.setUseCodeAsDefaultMessage(
                binder.bind("spring.messages.use-code-as-default-message", Boolean.class).orElse(false));
        Duration cacheDuration = binder.bind("spring.messages.cache-duration", Duration.class).orElse(null);
        if (cacheDuration != null) {
            source.setCacheMillis(cacheDuration.toMillis());
        } else if (binder.bind(MustacheReactProperties.PREFIX + ".dev-server.enabled", Boolean.class).orElse(false)) {
            source.setCacheMillis(1000); // development: edits show up on the next request
        }
        return source;
    }

    static List<String> basenames(Environment environment) {
        String names = environment.getProperty("spring.messages.basename", "messages");
        return new ArrayList<>(StringUtils.commaDelimitedListToSet(StringUtils.trimAllWhitespace(names)));
    }

    static ResourcePatternResolver resolver(ResourceLoader resourceLoader) {
        return ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
    }

    /** YAML bundles exist and no {@code <basename>.properties} does (Spring Boot would create its own then). */
    static class OnlyYamlBundles implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ResourcePatternResolver resolver = resolver(context.getResourceLoader());
            List<String> basenames = basenames(context.getEnvironment());
            if (!YamlMessageSource.exists(resolver, basenames)) {
                return false;
            }
            for (String basename : basenames) {
                String path = basename.replaceFirst("^classpath\\*?:", "").replaceFirst("^/+", "").replace('.', '/');
                try {
                    for (org.springframework.core.io.Resource resource
                            : resolver.getResources("classpath*:" + path + ".properties")) {
                        if (resource.exists()) {
                            return false;
                        }
                    }
                } catch (java.io.IOException e) {
                    return false;
                }
            }
            return true;
        }
    }
}
