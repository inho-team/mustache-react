package run.iknow.mustachereact.examples.basic.orders;

import java.time.LocalDate;

public class Order {

    private final long id;
    private final String customer;
    private final long amount;
    private final OrderStatus status;
    private final LocalDate orderedAt;

    public Order(long id, String customer, long amount, OrderStatus status, LocalDate orderedAt) {
        this.id = id;
        this.customer = customer;
        this.amount = amount;
        this.status = status;
        this.orderedAt = orderedAt;
    }

    public long getId() {
        return id;
    }

    public String getCustomer() {
        return customer;
    }

    public long getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDate getOrderedAt() {
        return orderedAt;
    }
}
