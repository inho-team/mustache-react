package run.iknow.mustachereact.examples.basic.orders;

import java.util.List;

public class OrderListResponse {

    private final List<Order> orders;
    private final int total;

    public OrderListResponse(List<Order> orders) {
        this.orders = orders;
        this.total = orders.size();
    }

    public List<Order> getOrders() {
        return orders;
    }

    public int getTotal() {
        return total;
    }
}
