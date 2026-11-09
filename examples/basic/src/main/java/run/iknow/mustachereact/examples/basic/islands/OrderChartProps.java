package run.iknow.mustachereact.examples.basic.islands;

import java.util.List;

import run.iknow.mustachereact.examples.basic.orders.Order;
import run.iknow.mustachereact.spring.ReactIsland;

/** Props of islands/OrderChart.tsx, see {@link StatusBadgeProps}. */
@ReactIsland("OrderChart")
public class OrderChartProps {

    private List<Order> orders;
    private String currency;

    public List<Order> getOrders() {
        return orders;
    }

    public String getCurrency() {
        return currency;
    }
}
