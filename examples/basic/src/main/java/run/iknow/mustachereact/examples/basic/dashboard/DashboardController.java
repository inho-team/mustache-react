package run.iknow.mustachereact.examples.basic.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import run.iknow.mustachereact.examples.basic.orders.OrderRepository;

/**
 * A classic Mustache view. The template places React islands with {@code {{#react}}...{{/react}}};
 * the controller only fills the model, as usual.
 */
@Controller
public class DashboardController {

    private final OrderRepository orders;

    public DashboardController(OrderRepository orders) {
        this.orders = orders;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("title", "대시보드");
        model.addAttribute("orders", orders.findAll());
        model.addAttribute("currency", "원");
        return "dashboard";
    }
}
