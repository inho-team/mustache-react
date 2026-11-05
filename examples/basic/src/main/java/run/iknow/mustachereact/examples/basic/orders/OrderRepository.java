package run.iknow.mustachereact.examples.basic.orders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Repository;

/** In-memory sample data. */
@Repository
public class OrderRepository {

    private static final List<Order> ORDERS = Collections.unmodifiableList(Arrays.asList(
            new Order(1001, "김민수", 32000, OrderStatus.PAID, LocalDate.of(2026, 11, 1)),
            new Order(1002, "이서연", 158000, OrderStatus.SHIPPED, LocalDate.of(2026, 11, 2)),
            new Order(1003, "박지훈", 7500, OrderStatus.CANCELLED, LocalDate.of(2026, 11, 3)),
            new Order(1004, "</script><script>alert(1)</script>", 1, OrderStatus.PAID, LocalDate.of(2026, 11, 4))));

    public List<Order> findAll() {
        return ORDERS;
    }
}
