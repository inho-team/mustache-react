package run.iknow.mustachereact.examples.basic.islands;

import run.iknow.mustachereact.examples.basic.orders.OrderStatus;
import run.iknow.mustachereact.spring.ReactIsland;

/**
 * Props of islands/StatusBadge.tsx. Templates pass them ({@code <react:StatusBadge status="${order.status}"
 * order-id="${order.id}" />}); this class declares them, so the component gets {@code IslandProps<'StatusBadge'>}
 * and a template passing a wrong or missing prop fails with a clear message.
 */
@ReactIsland("StatusBadge")
public class StatusBadgeProps {

    private OrderStatus status;
    private long orderId;

    public OrderStatus getStatus() {
        return status;
    }

    public long getOrderId() {
        return orderId;
    }
}
