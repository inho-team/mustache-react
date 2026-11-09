package run.iknow.mustachereact.spring.thymeleaf;

import java.util.HashSet;
import java.util.Set;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.StandardXmlNsTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;

import run.iknow.mustachereact.spring.IslandRenderer;

/**
 * React islands in Thymeleaf templates: {@code <react:OrderChart orders="${orders}" />} renders
 * {@code src/main/frontend/islands/OrderChart.tsx} with the given props.
 *
 * <pre>
 * &lt;head&gt; ... &lt;react:head /&gt; &lt;/head&gt;
 * &lt;body&gt;
 *   &lt;h1 th:text="#{orders.title}"&gt;Orders&lt;/h1&gt;
 *   &lt;react:OrderChart orders="${orders}" currency="KRW" /&gt;
 *   &lt;tr th:each="order : ${orders}"&gt;
 *     &lt;td&gt;&lt;react:StatusBadge status="${order.status}" order-id="${order.id}" /&gt;&lt;/td&gt;
 *   &lt;/tr&gt;
 *   &lt;react:scripts /&gt;
 * &lt;/body&gt;
 * </pre>
 *
 * <p>Attribute values written as Thymeleaf expressions ({@code ${...}}, {@code *{...}}, {@code #{...}},
 * {@code @{...}}) are evaluated and passed as objects (lists, DTOs, numbers); other values are passed as strings.
 * {@code order-id} becomes the prop {@code orderId}. {@code th:if}, {@code th:each} and {@code th:with} work on the
 * tag. {@code <react:island component="charts/OrderChart" ... />} names a component in a subdirectory.
 *
 * <p>Its processors run after the Standard Dialect's, so a {@code th:each} on the tag repeats the island.
 */
public class MustacheReactDialect extends AbstractProcessorDialect {

    /** The default prefix: {@code <react:Name>}. */
    public static final String PREFIX = "react";

    private final IslandRenderer islands;

    /**
     * @param islands renders the islands and the asset tags
     */
    public MustacheReactDialect(IslandRenderer islands) {
        this(islands, PREFIX);
    }

    /**
     * @param islands renders the islands and the asset tags
     * @param prefix element prefix, e.g. {@code react} for {@code <react:Name>}
     */
    public MustacheReactDialect(IslandRenderer islands, String prefix) {
        super("mustache-react", prefix, StandardDialect.PROCESSOR_PRECEDENCE + 1000);
        this.islands = islands;
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        Set<IProcessor> processors = new HashSet<>();
        processors.add(new IslandElementProcessor(dialectPrefix, islands));
        // Removes xmlns:react="..." from the output, as the Standard Dialect does with xmlns:th.
        processors.add(new StandardXmlNsTagProcessor(TemplateMode.HTML, dialectPrefix));
        return processors;
    }
}
