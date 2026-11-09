package run.iknow.mustachereact.examples.basic.board;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import run.iknow.mustachereact.examples.basic.orders.OrderRepository;

/**
 * A classic Thymeleaf view: the template (templates/board.html) places React islands with
 * {@code <react:OrderChart orders="${orders}" />}; the controller only fills the model, as usual.
 */
@Controller
public class BoardController {

    private final OrderRepository orders;

    public BoardController(OrderRepository orders) {
        this.orders = orders;
    }

    @GetMapping("/board")
    public String board(Model model) {
        model.addAttribute("orders", orders.findAll());
        model.addAttribute("currency", "원");
        return "board";
    }
}
