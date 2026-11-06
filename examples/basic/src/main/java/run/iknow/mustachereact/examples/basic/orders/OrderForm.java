package run.iknow.mustachereact.examples.basic.orders;

import org.springframework.lang.Nullable;

/** What the order form submits (JSON, bound with {@code @RequestBody}). */
public class OrderForm {

    private String customer = "";
    private Long amount;

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    /** Empty until the user types a number. */
    @Nullable
    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}
