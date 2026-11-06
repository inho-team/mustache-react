package run.iknow.mustachereact.spring;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.MessageSource;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Puts {@link ReactPageReturnValueHandler} first in the {@link RequestMappingHandlerAdapter}'s return value handlers.
 *
 * <p>It must come before the built-in handlers: otherwise {@code @RestController} methods would be handled by the
 * {@code @ResponseBody} handler before {@code @ReactPage} is ever seen. ({@code WebMvcConfigurer#addReturnValueHandlers}
 * only appends after the built-in ones.)
 */
public class ReactPageHandlerAdapterPostProcessor implements BeanPostProcessor {

    private final ObjectProvider<ReactPageRenderer> renderer;
    private final ObjectProvider<MessageSource> messageSource;

    /**
     * @param renderer renders the HTML page; looked up lazily, on the first request
     * @param messageSource resolves validation messages; looked up lazily
     */
    public ReactPageHandlerAdapterPostProcessor(ObjectProvider<ReactPageRenderer> renderer,
            ObjectProvider<MessageSource> messageSource) {
        this.renderer = renderer;
        this.messageSource = messageSource;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof RequestMappingHandlerAdapter) {
            install((RequestMappingHandlerAdapter) bean);
        }
        return bean;
    }

    private void install(RequestMappingHandlerAdapter adapter) {
        List<HandlerMethodReturnValueHandler> current = adapter.getReturnValueHandlers();
        if (current == null) {
            return;
        }
        HandlerMethodReturnValueHandler entityHandler = null;
        for (HandlerMethodReturnValueHandler handler : current) {
            if (handler instanceof ReactPageReturnValueHandler) {
                return; // already installed
            }
            // Spring 5.3: HttpEntityMethodProcessor. Spring 6+: ResponseEntityReturnValueHandler, which wraps it.
            if (entityHandler == null && ReactPageReturnValueHandler.handlesResponseEntity(handler)) {
                entityHandler = handler;
            }
        }
        if (entityHandler == null) {
            List<String> names = new ArrayList<>();
            for (HandlerMethodReturnValueHandler handler : current) {
                names.add(handler.getClass().getName());
            }
            throw new IllegalStateException("mustache-react: no Spring MVC return value handler for ResponseEntity "
                    + "was found among " + names);
        }
        List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>(current.size() + 1);
        handlers.add(new ReactPageReturnValueHandler(entityHandler,
                new PropsSerializer(adapter.getMessageConverters()), renderer, messageSource));
        handlers.addAll(current);
        adapter.setReturnValueHandlers(handlers);
    }
}
