package run.iknow.mustachereact.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the props of an island, for both sides of the template:
 *
 * <pre>{@code
 * @ReactIsland("StatusBadge")
 * public class StatusBadgeProps {
 *     private OrderStatus status;
 *     private long orderId;
 *     // getters
 * }
 * }</pre>
 *
 * <ul>
 *   <li>{@code mustache-react-processor} writes {@code IslandProps<'StatusBadge'>} into {@code types.ts}, so the
 *       component in {@code islands/StatusBadge.tsx} is type-checked.</li>
 *   <li>When a Mustache template renders the island ({@code <react:StatusBadge ...>} or
 *       {@code {{#react}}StatusBadge ...{{/react}}} in Mustache), a prop the class does not have, or a missing
 *       prop that is not {@code @Nullable}, fails the rendering with a message naming the island and its props.</li>
 * </ul>
 *
 * <p>The class is only a declaration: templates still pass the values themselves.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ReactIsland {

    /**
     * @return the island name: the file under {@code src/main/frontend/islands} without extension, e.g.
     *     {@code StatusBadge} or {@code charts/OrderChart}
     */
    String value();
}
