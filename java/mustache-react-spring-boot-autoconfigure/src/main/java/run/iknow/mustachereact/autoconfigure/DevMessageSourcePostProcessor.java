package run.iknow.mustachereact.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.support.AbstractResourceBasedMessageSource;
import org.springframework.core.env.Environment;

/**
 * In development mode ({@code mustache-react.dev-server.enabled=true}), lets the application's message source read
 * changed bundles after one second, unless {@code spring.messages.cache-duration} says otherwise. Edits to
 * {@code messages.properties} then show up on the next request once the file is on the classpath (the IDE's build,
 * or Gradle's {@code processResources}).
 */
class DevMessageSourcePostProcessor implements BeanPostProcessor {

    private final Environment environment;

    DevMessageSourcePostProcessor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AbstractResourceBasedMessageSource && "messageSource".equals(beanName)
                && environment.getProperty(MustacheReactProperties.PREFIX + ".dev-server.enabled", Boolean.class, false)
                && !environment.containsProperty("spring.messages.cache-duration")) {
            ((AbstractResourceBasedMessageSource) bean).setCacheMillis(1000);
        }
        return bean;
    }
}
