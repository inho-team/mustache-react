package run.iknow.mustachereact.spring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Server context entries from Spring Security (5.7 to 7). Only used when Spring Security is on the class path.
 */
public final class SecurityContextContributors {

    private SecurityContextContributors() {
    }

    /**
     * {@code csrf}: {@code {headerName, parameterName, token}} of the request's CSRF token, so that {@code fetch}
     * calls and forms can send it. Absent when CSRF protection is off.
     *
     * @return the contributor
     */
    public static ServerContextContributor csrf() {
        return context -> {
            RequestAttributes request = RequestContextHolder.getRequestAttributes();
            Object token = request == null ? null
                    : request.getAttribute(CsrfToken.class.getName(), RequestAttributes.SCOPE_REQUEST);
            if (token instanceof CsrfToken) {
                CsrfToken csrf = (CsrfToken) token;
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("headerName", csrf.getHeaderName());
                value.put("parameterName", csrf.getParameterName());
                value.put("token", csrf.getToken());
                context.put("csrf", value);
            }
        };
    }

    /**
     * {@code user}: {@code {name, authorities}} of the signed-in user, {@code null} for anonymous visitors.
     *
     * @return the contributor
     */
    public static ServerContextContributor user() {
        return context -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                context.put("user", null);
                return;
            }
            List<String> authorities = new ArrayList<>();
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                authorities.add(authority.getAuthority());
            }
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("name", authentication.getName());
            user.put("authorities", authorities);
            context.put("user", user);
        };
    }
}
