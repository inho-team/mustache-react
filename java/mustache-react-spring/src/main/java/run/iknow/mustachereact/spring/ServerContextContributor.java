package run.iknow.mustachereact.spring;

import java.util.Map;

/**
 * Adds entries to the server context every page receives ({@code useServerContext()} in React).
 *
 * <p>Declare a bean to expose more, for example the tenant or feature flags. Runs on the request thread, so
 * {@link org.springframework.web.context.request.RequestContextHolder} and the security context are available.
 * Everything added is written into the page as JSON: never add secrets.
 */
@FunctionalInterface
public interface ServerContextContributor {

    /**
     * @param context the context being built; add entries to it
     */
    void contribute(Map<String, Object> context);
}
