package run.iknow.mustachereact.spring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * Collects the {@code @Valid} errors of a handler call ({@link BindingResult}s in the model) as
 * {@code {fields: {name: [messages]}, global: [messages]}}, with messages resolved in the request locale.
 */
final class ValidationErrors {

    private ValidationErrors() {
    }

    /**
     * @return the errors, or {@code null} when there are none
     */
    static Map<String, Object> collect(Map<String, Object> model, MessageSource messages, Locale locale) {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        List<String> global = new ArrayList<>();
        for (Object value : model.values()) {
            if (!(value instanceof BindingResult) || !((BindingResult) value).hasErrors()) {
                continue;
            }
            for (ObjectError error : ((BindingResult) value).getAllErrors()) {
                String message = messages != null ? messages.getMessage(error, locale)
                        : (error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getCode());
                if (error instanceof FieldError) {
                    String field = ((FieldError) error).getField();
                    List<String> list = fields.get(field);
                    if (list == null) {
                        list = new ArrayList<>();
                        fields.put(field, list);
                    }
                    list.add(message);
                } else {
                    global.add(message);
                }
            }
        }
        if (fields.isEmpty() && global.isEmpty()) {
            return null;
        }
        Map<String, Object> errors = new LinkedHashMap<>();
        errors.put("fields", fields);
        errors.put("global", global);
        return errors;
    }
}
