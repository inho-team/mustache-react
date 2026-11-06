package run.iknow.mustachereact.examples.basic.orders;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validates {@link OrderForm}. Messages come from {@code messages.properties} through the error codes.
 *
 * <p>An application on one Spring Boot version would usually put {@code @NotBlank} / {@code @Min} on the form and use
 * {@code @Valid}; the example runs on Boot 2.7 (javax.validation) and 3+ (jakarta.validation) from one source, so it
 * uses a Spring {@link Validator}. Both end up in the same {@code BindingResult}.
 */
public class OrderFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> type) {
        return OrderForm.class.isAssignableFrom(type);
    }

    @Override
    public void validate(Object target, Errors errors) {
        OrderForm form = (OrderForm) target;
        if (form.getCustomer() == null || form.getCustomer().trim().isEmpty()) {
            errors.rejectValue("customer", "required", "Enter the customer");
        }
        if (form.getAmount() == null || form.getAmount() < 1) {
            errors.rejectValue("amount", "min", new Object[] {1}, "The amount must be at least 1");
        }
    }
}
