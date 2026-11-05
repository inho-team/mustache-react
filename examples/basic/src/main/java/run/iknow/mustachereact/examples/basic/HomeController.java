package run.iknow.mustachereact.examples.basic;

import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import run.iknow.mustachereact.core.MustacheReactVersion;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("bootVersion", SpringBootVersion.getVersion());
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("mustacheReactVersion", MustacheReactVersion.get());
        return "index";
    }
}
