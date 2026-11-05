package run.iknow.mustachereact.examples.basic.orders;

import java.time.LocalDate;
import java.util.Arrays;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import run.iknow.mustachereact.spring.ReactPage;

/**
 * An ordinary API controller. {@code @ReactPage} adds a page for browsers; API clients still get JSON.
 */
@RestController
public class OrderController {

    @ReactPage(value = "orders/OrderList", title = "주문 목록")
    @GetMapping("/orders")
    public OrderListResponse list() {
        return new OrderListResponse(Arrays.asList(
                new Order(1001, "김민수", 32000, OrderStatus.PAID, LocalDate.of(2026, 11, 1)),
                new Order(1002, "이서연", 158000, OrderStatus.SHIPPED, LocalDate.of(2026, 11, 2)),
                new Order(1003, "박지훈", 7500, OrderStatus.CANCELLED, LocalDate.of(2026, 11, 3)),
                new Order(1004, "</script><script>alert(1)</script>", 1, OrderStatus.PAID, LocalDate.of(2026, 11, 4))));
    }
}
