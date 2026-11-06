package run.iknow.mustachereact.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Asks {@code mustache-react-processor} to write a TypeScript type for this class, for example the props of an
 * island used from Mustache:
 *
 * <pre>{@code
 * @ReactType
 * public class ChartProps { ... }
 * }</pre>
 *
 * <p>Return types of {@link ReactPage} methods, and every class they reference, get a type without this annotation.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ReactType {
}
