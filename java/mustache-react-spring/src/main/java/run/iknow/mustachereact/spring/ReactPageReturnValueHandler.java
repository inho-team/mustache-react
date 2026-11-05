package run.iknow.mustachereact.spring;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Handles the return value of {@link ReactPage} methods.
 *
 * <p>Both outcomes are written by Spring MVC's own {@link ResponseEntity} handler ({@code HttpEntityMethodProcessor}
 * on Spring 5.3, {@code ResponseEntityReturnValueHandler} on Spring 6+):
 * the HTML page as {@code ResponseEntity<String>}, or the data as JSON. This keeps the code free of
 * {@code javax.servlet} / {@code jakarta.servlet} types, so one binary runs on Spring 5.3, 6.x and 7.x, and keeps
 * {@code ResponseBodyAdvice}, message converters and content negotiation working as usual.
 */
public class ReactPageReturnValueHandler implements HandlerMethodReturnValueHandler {

    private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

    private static final MethodParameter HTML_RETURN_TYPE = returnTypeOf("htmlResponse");
    private static final MethodParameter DATA_RETURN_TYPE = returnTypeOf("dataResponse");

    private final HandlerMethodReturnValueHandler entityHandler;
    private final PropsSerializer propsSerializer;
    private final ObjectProvider<ReactPageRenderer> renderer;

    /**
     * @param entityHandler Spring MVC's handler for {@code ResponseEntity} return values, see {@link #handlesResponseEntity}
     * @param propsSerializer turns return values into props JSON
     * @param renderer renders the HTML page; looked up on first use
     */
    public ReactPageReturnValueHandler(HandlerMethodReturnValueHandler entityHandler, PropsSerializer propsSerializer,
            ObjectProvider<ReactPageRenderer> renderer) {
        this.entityHandler = entityHandler;
        this.propsSerializer = propsSerializer;
        this.renderer = renderer;
    }

    /**
     * Whether the given handler is Spring MVC's handler for {@link ResponseEntity} return values.
     */
    static boolean handlesResponseEntity(HandlerMethodReturnValueHandler handler) {
        return !(handler instanceof ReactPageReturnValueHandler)
                && handler.supportsReturnType(HTML_RETURN_TYPE)
                && handler.supportsReturnType(DATA_RETURN_TYPE);
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return returnType.hasMethodAnnotation(ReactPage.class);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest) throws Exception {
        if (returnValue instanceof ResponseEntity) {
            throw new IllegalStateException("@ReactPage method " + returnType.getMethod()
                    + " returned a ResponseEntity. Return the props object (DTO or Map) directly.");
        }
        ReactPageRenderer pages = renderer.getObject();

        if (pages.isContentNegotiation() && !AcceptHeaders.prefersHtml(webRequest.getHeaderValues(HttpHeaders.ACCEPT))) {
            ResponseEntity<Object> data = ResponseEntity.ok().header(HttpHeaders.VARY, HttpHeaders.ACCEPT).body(returnValue);
            entityHandler.handleReturnValue(data, DATA_RETURN_TYPE, mavContainer, webRequest);
            return;
        }

        ReactPage page = returnType.getMethodAnnotation(ReactPage.class);
        String html = pages.render(page.value(), page.title(), propsSerializer.serialize(returnValue),
                LocaleContextHolder.getLocale());
        ResponseEntity<String> response = ResponseEntity.ok()
                .contentType(TEXT_HTML_UTF8)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT)
                .body(html);
        entityHandler.handleReturnValue(response, HTML_RETURN_TYPE, mavContainer, webRequest);
    }

    // Declared return types handed to HttpEntityMethodProcessor instead of the @ReactPage method's own.
    @SuppressWarnings("unused")
    private static ResponseEntity<String> htmlResponse() {
        return null;
    }

    @SuppressWarnings("unused")
    private static ResponseEntity<Object> dataResponse() {
        return null;
    }

    private static MethodParameter returnTypeOf(String methodName) {
        try {
            Method method = ReactPageReturnValueHandler.class.getDeclaredMethod(methodName);
            return new MethodParameter(method, -1);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
