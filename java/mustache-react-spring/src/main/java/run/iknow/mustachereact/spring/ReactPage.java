package run.iknow.mustachereact.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Renders the return value of a Spring MVC handler method as a React page.
 *
 * <pre>{@code
 * @ReactPage("orders/OrderList")
 * @GetMapping("/orders")
 * public OrderListResponse list(Pageable pageable) {
 *     return orderService.list(pageable);
 * }
 * }</pre>
 *
 * <p>A browser request ({@code Accept: text/html}) gets an HTML page in which the returned object becomes the props
 * of the component {@code src/main/frontend/pages/orders/OrderList.tsx}. A request for {@code application/json}
 * gets the same object as JSON, so an existing API endpoint keeps working.
 *
 * <p><strong>Status:</strong> declared in M0; the rendering is implemented in M1.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReactPage {

    /**
     * Component name, relative to {@code src/main/frontend/pages} and without extension,
     * for example {@code "orders/OrderList"}.
     *
     * @return the page component name
     */
    String value();

    /**
     * Page title. Empty means the layout default.
     *
     * @return the page title
     */
    String title() default "";
}
