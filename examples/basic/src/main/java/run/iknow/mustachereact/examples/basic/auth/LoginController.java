package run.iknow.mustachereact.examples.basic.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import run.iknow.mustachereact.spring.ReactPage;

/** The login page is a React page; Spring Security handles POST /login as usual. */
@Controller
public class LoginController {

    @ReactPage(value = "auth/Login", title = "로그인")
    @GetMapping("/login")
    public LoginPage login(@RequestParam(name = "error", required = false) String error) {
        return new LoginPage(error != null);
    }
}
