package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

class ValidationErrorsTest {

    public static class Form {
        public String getCustomer() {
            return "";
        }
    }

    @Test
    void collectsFieldAndGlobalErrorsWithLocalizedMessages() {
        BindingResult result = new BeanPropertyBindingResult(new Form(), "form");
        result.rejectValue("customer", "NotBlank", "must not be blank");
        result.rejectValue("customer", "Size", "too short");
        result.reject("Total", "total too high");
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("form", new Form());
        model.put(BindingResult.MODEL_KEY_PREFIX + "form", result);
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("NotBlank", Locale.KOREAN, "필수 입력입니다");

        Map<String, Object> errors = ValidationErrors.collect(model, messages, Locale.KOREAN);

        assertThat(errors).containsEntry("fields",
                Collections.singletonMap("customer", Arrays.asList("필수 입력입니다", "too short")));
        assertThat(errors).containsEntry("global", Collections.singletonList("total too high"));
    }

    @Test
    void returnsNullWithoutErrors() {
        Map<String, Object> model = Collections.<String, Object>singletonMap(BindingResult.MODEL_KEY_PREFIX + "form",
                new BeanPropertyBindingResult(new Form(), "form"));

        assertThat(ValidationErrors.collect(model, null, Locale.ENGLISH)).isNull();
    }
}
