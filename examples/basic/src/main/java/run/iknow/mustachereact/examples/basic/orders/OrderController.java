package run.iknow.mustachereact.examples.basic.orders;

import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import run.iknow.mustachereact.spring.ReactPage;

/**
 * An ordinary API controller. {@code @ReactPage} adds pages for browsers; API clients still get JSON.
 */
@RestController
public class OrderController {

    private final OrderRepository orders;

    public OrderController(OrderRepository orders) {
        this.orders = orders;
    }

    @ReactPage(value = "orders/OrderList", title = "주문 목록")
    @GetMapping("/orders")
    public OrderListResponse list() {
        return new OrderListResponse(orders.findAll());
    }

    @InitBinder("orderForm")
    void validation(WebDataBinder binder) {
        binder.addValidators(new OrderFormValidator());
    }

    @ReactPage(value = "orders/OrderForm", title = "주문 등록")
    @GetMapping("/orders/new")
    public OrderFormPage newOrder() {
        return new OrderFormPage(new OrderForm());
    }

    /**
     * Spring MVC form handling as usual: on validation errors, show the form again (useSpringForm displays the
     * errors next to the fields); otherwise save and redirect.
     */
    @ReactPage(value = "orders/OrderForm", title = "주문 등록")
    @PostMapping("/orders/new")
    public Object create(@Validated @RequestBody OrderForm form, BindingResult result) {
        if (result.hasErrors()) {
            return new OrderFormPage(form);
        }
        orders.add(form);
        return "redirect:/orders";
    }
}
