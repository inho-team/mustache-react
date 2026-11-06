package run.iknow.mustachereact.autoconfigure;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Language switching with {@code mustache-react.i18n.param}: {@code ?lang=en} changes the language and a cookie
 * keeps it. Runs before Spring MVC's auto-configuration, whose {@code localeResolver} (the {@code Accept-Language}
 * header, which cannot be changed) it replaces. An application's own {@code localeResolver} bean is kept.
 */
@AutoConfiguration(beforeName = {
        // Spring Boot 2.7, 3.x
        "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration",
        // Spring Boot 4.x
        "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration",
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.i18n.LocaleChangeInterceptor")
@ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "i18n.param")
@EnableConfigurationProperties(MustacheReactProperties.class)
public class MustacheReactLocaleAutoConfiguration {

    /** Keeps the chosen language in a cookie; before a choice, the default locale or the Accept-Language header. */
    @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    @ConditionalOnMissingBean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    public LocaleResolver localeResolver(MustacheReactProperties properties) {
        MustacheReactProperties.I18n i18n = properties.getI18n();
        CookieLocaleResolver resolver = cookieLocaleResolver(i18n.getCookieName());
        setCookieMaxAge(resolver, i18n.getCookieMaxAge());
        if (i18n.getDefaultLocale() != null) {
            resolver.setDefaultLocale(i18n.getDefaultLocale());
        }
        return resolver;
    }

    /** {@code ?lang=en} sets the language of this request and the following ones. */
    @Bean
    public WebMvcConfigurer mustacheReactLocaleChange(MustacheReactProperties properties) {
        String param = properties.getI18n().getParam();
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
                interceptor.setParamName(param);
                interceptor.setIgnoreInvalidLocale(true);
                registry.addInterceptor(interceptor);
            }
        };
    }

    // Spring 5.3 sets the cookie name and age through CookieGenerator setters, Spring 6+ through a constructor and
    // a Duration setter. Reflection keeps one binary for both.

    private static CookieLocaleResolver cookieLocaleResolver(String cookieName) {
        try {
            Constructor<CookieLocaleResolver> named = CookieLocaleResolver.class.getConstructor(String.class);
            return named.newInstance(cookieName);
        } catch (NoSuchMethodException e) {
            CookieLocaleResolver resolver = new CookieLocaleResolver();
            invoke(resolver, "setCookieName", String.class, cookieName);
            return resolver;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create the CookieLocaleResolver", e);
        }
    }

    private static void setCookieMaxAge(CookieLocaleResolver resolver, Duration maxAge) {
        if (maxAge == null) {
            return;
        }
        try {
            resolver.getClass().getMethod("setCookieMaxAge", Duration.class).invoke(resolver, maxAge);
        } catch (NoSuchMethodException e) {
            invoke(resolver, "setCookieMaxAge", Integer.class, (int) maxAge.getSeconds());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set the locale cookie age", e);
        }
    }

    private static void invoke(Object target, String name, Class<?> type, Object value) {
        try {
            Method method = target.getClass().getMethod(name, type);
            method.invoke(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not call " + name + " on " + target.getClass().getName(), e);
        }
    }
}
