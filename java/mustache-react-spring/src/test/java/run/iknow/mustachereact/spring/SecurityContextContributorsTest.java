package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

class SecurityContextContributorsTest {

    @AfterEach
    void reset() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private static Map<String, Object> contribute(ServerContextContributor contributor) {
        Map<String, Object> context = new LinkedHashMap<>();
        contributor.contribute(context);
        return context;
    }

    @Test
    void exposesTheCsrfToken() {
        MapRequestAttributes request = new MapRequestAttributes();
        request.setAttribute(CsrfToken.class.getName(), new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "abc"),
                RequestAttributes.SCOPE_REQUEST);
        RequestContextHolder.setRequestAttributes(request);

        Map<String, Object> context = contribute(SecurityContextContributors.csrf());

        Map<String, Object> csrf = new LinkedHashMap<>();
        csrf.put("headerName", "X-CSRF-TOKEN");
        csrf.put("parameterName", "_csrf");
        csrf.put("token", "abc");
        assertThat(context).containsEntry("csrf", csrf);
    }

    @Test
    void omitsCsrfWhenThereIsNoToken() {
        RequestContextHolder.setRequestAttributes(new MapRequestAttributes());

        assertThat(contribute(SecurityContextContributors.csrf())).isEmpty();
    }

    @Test
    void exposesTheSignedInUserButNotAnonymousVisitors() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("kim", "secret",
                AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")));

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", "kim");
        user.put("authorities", Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
        assertThat(contribute(SecurityContextContributors.user())).containsEntry("user", user)
                .doesNotContainValue("secret");

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(contribute(SecurityContextContributors.user())).containsEntry("user", null);
    }
}
