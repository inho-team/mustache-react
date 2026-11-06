package run.iknow.mustachereact.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * Gives Mustache views an explicit charset ({@code text/html;charset=UTF-8}).
 *
 * <p>Spring Boot's Mustache views send {@code text/html} without a charset, so the servlet container picks one from
 * the response locale: UTF-8 for English, but ISO-8859-1 for Korean, Japanese and most other languages, which turns
 * their text into {@code ????}. An explicit {@code spring.mustache.servlet.content-type} is kept.
 */
class MustacheCharsetPostProcessor implements BeanPostProcessor {

    private final Environment environment;

    MustacheCharsetPostProcessor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof UrlBasedViewResolver && bean.getClass().getName().contains("Mustache")
                && !environment.containsProperty("spring.mustache.servlet.content-type")) {
            String charset = environment.getProperty("spring.mustache.charset", "UTF-8");
            ((UrlBasedViewResolver) bean).setContentType("text/html;charset=" + charset);
        }
        return bean;
    }
}
