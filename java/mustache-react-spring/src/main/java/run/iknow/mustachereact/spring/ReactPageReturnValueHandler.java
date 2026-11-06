package run.iknow.mustachereact.spring;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
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
 *
 * <p>Forms: a method may return {@code "redirect:/path"}. JSON requests ({@code useSpringForm}) then get
 * {@code {"redirect": "/path"}}, browsers a 303. When {@code @Valid} found errors (a {@code BindingResult} parameter
 * holds them), JSON requests get 400 with {@code {"errors": {fields, global}}}; browsers get the page again with the
 * errors in the server context.
 */
public class ReactPageReturnValueHandler implements HandlerMethodReturnValueHandler {

    private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

    private static final MethodParameter HTML_RETURN_TYPE = returnTypeOf("htmlResponse");
    private static final MethodParameter DATA_RETURN_TYPE = returnTypeOf("dataResponse");

    private final HandlerMethodReturnValueHandler entityHandler;
    private final PropsSerializer propsSerializer;
    private final ObjectProvider<ReactPageRenderer> renderer;
    private final ObjectProvider<MessageSource> messageSource;

    /**
     * @param entityHandler Spring MVC's handler for {@code ResponseEntity} return values, see {@link #handlesResponseEntity}
     * @param propsSerializer turns return values into props JSON
     * @param renderer renders the HTML page; looked up on first use
     * @param messageSource resolves validation messages
     */
    public ReactPageReturnValueHandler(HandlerMethodReturnValueHandler entityHandler, PropsSerializer propsSerializer,
            ObjectProvider<ReactPageRenderer> renderer, ObjectProvider<MessageSource> messageSource) {
        this.entityHandler = entityHandler;
        this.propsSerializer = propsSerializer;
        this.renderer = renderer;
        this.messageSource = messageSource;
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
        Locale locale = LocaleContextHolder.getLocale();
        String redirect = redirectLocation(returnValue, webRequest);
        Map<String, Object> errors = ValidationErrors.collect(mavContainer.getModel(), messageSource.getIfAvailable(), locale);
        boolean json = pages.isContentNegotiation()
                && !AcceptHeaders.prefersHtml(webRequest.getHeaderValues(HttpHeaders.ACCEPT));

        if (json) {
            ResponseEntity<Object> data;
            if (redirect != null) {
                // fetch would follow a 3xx itself; useSpringForm navigates when it sees this.
                data = ResponseEntity.ok().header(HttpHeaders.VARY, HttpHeaders.ACCEPT)
                        .body(Collections.<String, Object>singletonMap("redirect", redirect));
            } else if (errors != null) {
                data = ResponseEntity.status(400).header(HttpHeaders.VARY, HttpHeaders.ACCEPT)
                        .body(Collections.<String, Object>singletonMap("errors", errors));
            } else {
                data = ResponseEntity.ok().header(HttpHeaders.VARY, HttpHeaders.ACCEPT).body(returnValue);
            }
            entityHandler.handleReturnValue(data, DATA_RETURN_TYPE, mavContainer, webRequest);
            return;
        }

        if (redirect != null) {
            // 303: the browser follows with a GET, as after a classic form post.
            ResponseEntity<String> see = ResponseEntity.status(303).header(HttpHeaders.LOCATION, redirect).build();
            entityHandler.handleReturnValue(see, HTML_RETURN_TYPE, mavContainer, webRequest);
            return;
        }
        ReactPage page = returnType.getMethodAnnotation(ReactPage.class);
        Map<String, Object> extraContext = errors == null ? null
                : Collections.<String, Object>singletonMap("errors", errors);
        String html = pages.render(page.value(), page.title(), propsSerializer.serialize(returnValue), extraContext, locale);
        ResponseEntity<String> response = ResponseEntity.ok()
                .contentType(TEXT_HTML_UTF8)
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT)
                .body(html);
        entityHandler.handleReturnValue(response, HTML_RETURN_TYPE, mavContainer, webRequest);
    }

    /**
     * {@code "redirect:/orders"} returned by a {@code @ReactPage} method means: redirect, as for a Spring MVC view
     * name. Paths starting with {@code /} are relative to the context path.
     */
    static String redirectLocation(Object returnValue, NativeWebRequest request) {
        if (!(returnValue instanceof CharSequence)) {
            return null;
        }
        String value = returnValue.toString();
        if (!value.startsWith(REDIRECT_PREFIX)) {
            return null;
        }
        String target = value.substring(REDIRECT_PREFIX.length());
        String contextPath = request.getContextPath();
        return target.startsWith("/") && !target.startsWith("//") && contextPath != null ? contextPath + target : target;
    }

    private static final String REDIRECT_PREFIX = "redirect:";

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
