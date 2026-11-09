package run.iknow.mustachereact.spring.thymeleaf;

import java.util.LinkedHashMap;
import java.util.Map;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.AbstractProcessor;
import org.thymeleaf.processor.element.IElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.processor.element.MatchingAttributeName;
import org.thymeleaf.processor.element.MatchingElementName;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import run.iknow.mustachereact.spring.IslandRenderer;

/**
 * Replaces {@code <react:Name ... />}, {@code <react:island component="dir/Name" ... />}, {@code <react:head />}
 * and {@code <react:scripts />}, see {@link MustacheReactDialect}.
 */
class IslandElementProcessor extends AbstractProcessor implements IElementTagProcessor {

    static final String HEAD = "head";
    static final String SCRIPTS = "scripts";
    static final String ISLAND = "island";
    static final String COMPONENT = "component";

    private final String prefix;
    private final IslandRenderer islands;
    private final MatchingElementName matchingElementName;

    IslandElementProcessor(String prefix, IslandRenderer islands) {
        super(TemplateMode.HTML, 1000);
        this.prefix = prefix;
        this.islands = islands;
        this.matchingElementName = MatchingElementName.forAllElementsWithPrefix(TemplateMode.HTML, prefix);
    }

    @Override
    public MatchingElementName getMatchingElementName() {
        return matchingElementName;
    }

    @Override
    public MatchingAttributeName getMatchingAttributeName() {
        return null;
    }

    @Override
    public void process(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structure) {
        String completeName = tag.getElementCompleteName();
        String localName = completeName.substring(completeName.indexOf(':') + 1);
        String html;
        try {
            if (localName.equalsIgnoreCase(HEAD)) {
                html = islands.head();
            } else if (localName.equalsIgnoreCase(SCRIPTS)) {
                html = islands.scripts();
            } else {
                boolean named = localName.equalsIgnoreCase(ISLAND);
                String component = named ? tag.getAttributeValue(COMPONENT) : componentName(localName);
                if (component == null || component.isEmpty()) {
                    throw new IllegalArgumentException("<" + completeName + "> needs component=\"dir/Name\"");
                }
                html = islands.render(component, props(context, tag, named));
            }
        } catch (TemplateProcessingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new TemplateProcessingException("<" + completeName + ">: " + e.getMessage(), e);
        }
        IModelFactory models = context.getModelFactory();
        IModel model = models.createModel();
        model.add(models.createText(html));
        structure.replaceWith(model, false);
    }

    private Map<String, Object> props(ITemplateContext context, IProcessableElementTag tag, boolean named) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (IAttribute attribute : tag.getAllAttributes()) {
            AttributeName name = attribute.getAttributeDefinition().getAttributeName();
            String completeName = attribute.getAttributeCompleteName();
            // th:* attributes were handled by the Standard Dialect before this processor; xmlns:* declare prefixes.
            if (name.getPrefix() != null || completeName.startsWith("data-th-") || completeName.startsWith("xmlns")
                    || (named && completeName.equalsIgnoreCase(COMPONENT))) {
                continue;
            }
            props.put(propName(completeName), value(context, attribute.getValue()));
        }
        return props;
    }

    /** {@code ${...}}, {@code *{...}}, {@code #{...}}, {@code @{...}} are evaluated; anything else is a string. */
    static Object value(ITemplateContext context, String value) {
        if (value == null) {
            return Boolean.TRUE; // <react:Toggle open />
        }
        String trimmed = value.trim();
        if (trimmed.length() > 3 && "$*#@".indexOf(trimmed.charAt(0)) >= 0 && trimmed.charAt(1) == '{'
                && trimmed.endsWith("}")) {
            IStandardExpression expression = StandardExpressions.getExpressionParser(context.getConfiguration())
                    .parseExpression(context, trimmed);
            return expression.execute(context);
        }
        return value;
    }

    /** {@code order-id} and {@code orderId} are the prop {@code orderId}. */
    static String propName(String attribute) {
        StringBuilder name = new StringBuilder(attribute.length());
        boolean upper = false;
        for (char c : attribute.toCharArray()) {
            if (c == '-' || c == '_') {
                upper = name.length() > 0;
            } else {
                name.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return name.toString();
    }

    /** {@code OrderChart} as written; {@code order-chart} becomes {@code OrderChart}. */
    static String componentName(String localName) {
        if (localName.indexOf('-') < 0) {
            return localName;
        }
        String camel = propName(localName);
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }
}
