package run.iknow.mustachereact.examples.basic.orders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

/** In-memory sample data. */
@Repository
public class OrderRepository {

    private final List<Order> orders = new CopyOnWriteArrayList<>(Arrays.asList(
            new Order(1001, "김민수", 32000, OrderStatus.PAID, LocalDate.of(2026, 11, 1)),
            new Order(1002, "이서연", 158000, OrderStatus.SHIPPED, LocalDate.of(2026, 11, 2)),
            new Order(1003, "박지훈", 7500, OrderStatus.CANCELLED, LocalDate.of(2026, 11, 3)),
            new Order(1004, "</script><script>alert(1)</script>", 1, OrderStatus.PAID, LocalDate.of(2026, 11, 4))));

    private final AtomicLong nextId = new AtomicLong(1005);

    public List<Order> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(orders));
    }

    public Optional<Order> findById(long id) {
        for (Order order : orders) {
            if (order.getId() == id) {
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    public Order add(OrderForm form) {
        Order order = new Order(nextId.getAndIncrement(), form.getCustomer().trim(), form.getAmount(), OrderStatus.PAID,
                LocalDate.now());
        orders.add(order);
        return order;
    }
}
