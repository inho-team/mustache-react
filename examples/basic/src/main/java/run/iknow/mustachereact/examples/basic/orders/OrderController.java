package run.iknow.mustachereact.examples.basic.orders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import run.iknow.mustachereact.spring.ReactPage;

/**
 * An ordinary API controller. {@code @ReactPage} adds a page for browsers; API clients still get JSON.
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
}
