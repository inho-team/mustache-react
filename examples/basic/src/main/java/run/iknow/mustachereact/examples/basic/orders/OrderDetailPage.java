package run.iknow.mustachereact.examples.basic.orders;

import org.springframework.lang.Nullable;

/** Props of the order detail page; {@code order} is null when there is no such order (404). */
public class OrderDetailPage {

    private final long id;
    private final Order order;

    public OrderDetailPage(long id, Order order) {
        this.id = id;
        this.order = order;
    }

    public long getId() {
        return id;
    }

    @Nullable
    public Order getOrder() {
        return order;
    }
}
