package run.iknow.mustachereact.spring;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.context.request.RequestAttributes;

/** Test helpers of this package for the Thymeleaf tests. */
public final class ThymeleafTestSupport {

    private ThymeleafTestSupport() {
    }

    public static RequestAttributes requestAttributes() {
        return new MapRequestAttributes();
    }

    public static HttpMessageConverter<?> jsonConverter() {
        return new ReactIslandLambdaTest.MapJsonConverter();
    }
}
