package run.iknow.mustachereact.spring;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds the {@code react} section and the {@code mr} helpers to every Spring MVC model, so any Mustache view can use
 * {@code {{#react}}...{{/react}}}, {@code {{{mr.head}}}} and {@code {{{mr.scripts}}}} without controller code.
 *
 * <p>Attributes the application already put in the model under the same names are left alone.
 */
@ControllerAdvice
public class MustacheReactModelAdvice {

    private final String reactName;
    private final ReactIslandLambda react;
    private final String helpersName;
    private final MustacheReactHelpers helpers;

    /**
     * @param reactName model name of the island section, usually {@code react}
     * @param react the island section
     * @param helpersName model name of the helpers, usually {@code mr}
     * @param helpers the helpers
     */
    public MustacheReactModelAdvice(String reactName, ReactIslandLambda react, String helpersName,
            MustacheReactHelpers helpers) {
        this.reactName = reactName;
        this.react = react;
        this.helpersName = helpersName;
        this.helpers = helpers;
    }

    /**
     * Adds the attributes.
     *
     * @param model the model of the current request
     */
    @ModelAttribute
    public void addMustacheReactAttributes(Model model) {
        if (!model.containsAttribute(reactName)) {
            model.addAttribute(reactName, react);
        }
        if (!model.containsAttribute(helpersName)) {
            model.addAttribute(helpersName, helpers);
        }
    }
}
