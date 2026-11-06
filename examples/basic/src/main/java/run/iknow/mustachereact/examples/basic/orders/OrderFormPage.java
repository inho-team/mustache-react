package run.iknow.mustachereact.examples.basic.orders;

/** Props of the order form page. */
public class OrderFormPage {

    private final OrderForm form;

    public OrderFormPage(OrderForm form) {
        this.form = form;
    }

    public OrderForm getForm() {
        return form;
    }
}
