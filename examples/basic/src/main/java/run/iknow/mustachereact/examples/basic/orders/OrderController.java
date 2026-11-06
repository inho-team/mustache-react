package run.iknow.mustachereact.examples.basic.orders;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
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

    @ReactPage(value = "orders/OrderList", title = "orders.title")
    @GetMapping("/orders")
    public OrderListResponse list() {
        return new OrderListResponse(orders.findAll());
    }

    /**
     * A ResponseEntity sets the status and headers of the page (and of the JSON response); its body is the props.
     */
    @ReactPage(value = "orders/OrderDetail", title = "orders.detail.title")
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDetailPage> detail(@PathVariable("id") long id) {
        Optional<Order> order = orders.findById(id);
        if (!order.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new OrderDetailPage(id, null));
        }
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(new OrderDetailPage(id, order.get()));
    }

    @InitBinder("orderForm")
    void validation(WebDataBinder binder) {
        binder.addValidators(new OrderFormValidator());
    }

    @ReactPage(value = "orders/OrderForm", title = "orders.new")
    @GetMapping("/orders/new")
    public OrderFormPage newOrder() {
        return new OrderFormPage(new OrderForm());
    }

    /**
     * Spring MVC form handling as usual: on validation errors, show the form again (useSpringForm displays the
     * errors next to the fields); otherwise save and redirect.
     */
    @ReactPage(value = "orders/OrderForm", title = "orders.new")
    @PostMapping("/orders/new")
    public Object create(@Validated @RequestBody OrderForm form, BindingResult result) {
        if (result.hasErrors()) {
            return new OrderFormPage(form);
        }
        orders.add(form);
        return "redirect:/orders";
    }
}
