package run.iknow.mustachereact.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;

import com.samskivert.mustache.Mustache;

import run.iknow.mustachereact.spring.IslandTagCompiler;

/**
 * Lets Mustache views use island tags ({@code <react:StatusBadge status="{{status}}" />}) by wrapping the
 * application's {@link Mustache.Compiler} bean. Off with {@code mustache-react.template.html-tags=false}.
 */
class IslandTagCompilerPostProcessor implements BeanPostProcessor {

    private final Environment environment;

    IslandTagCompilerPostProcessor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof Mustache.Compiler) || bean instanceof IslandTagCompiler
                || !environment.getProperty(MustacheReactProperties.PREFIX + ".template.html-tags", Boolean.class, true)) {
            return bean;
        }
        String section = environment.getProperty(MustacheReactProperties.PREFIX + ".template.react-name", "react");
        String helpers = environment.getProperty(MustacheReactProperties.PREFIX + ".template.helpers-name", "mr");
        return new IslandTagCompiler((Mustache.Compiler) bean, section, section, helpers);
    }
}
