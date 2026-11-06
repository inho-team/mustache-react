package run.iknow.mustachereact.examples.basic.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Form login with a React login page. Everything is public except creating orders.
 * CSRF protection stays on: React pages send the token from the server context.
 *
 * <p>Written with APIs shared by Spring Security 5.7, 6 and 7, as the example runs on Boot 2.7 to 4.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                        .requestMatchers(request -> request.getRequestURI().startsWith("/orders/new")).authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form.loginPage("/login").permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/orders"));
        return http.build();
    }

    /** Demo user: kim / password. */
    @Bean
    @SuppressWarnings("deprecation")
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withDefaultPasswordEncoder().username("kim").password("password").roles("USER").build());
    }
}
