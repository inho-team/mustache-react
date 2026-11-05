package run.iknow.mustachereact.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Islands rendered so far while producing one response: gives each island a unique id and remembers which
 * components the page uses, so that {@code mr.scripts} can preload them.
 *
 * <p>Kept as a request attribute. Outside a request (a template rendered by hand) a fresh state is used for every
 * call: ids stay unique, but {@code mr.scripts} cannot know the islands.
 */
final class IslandRequestState {

    private static final String ATTRIBUTE = IslandRequestState.class.getName();
    private static final AtomicLong DETACHED_IDS = new AtomicLong();

    private final String idPrefix;
    private int next;
    private final Set<String> components = new LinkedHashSet<>();

    private IslandRequestState(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    static IslandRequestState current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new IslandRequestState("mr-x" + DETACHED_IDS.incrementAndGet() + "-");
        }
        Object state = attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (state instanceof IslandRequestState) {
            return (IslandRequestState) state;
        }
        IslandRequestState created = new IslandRequestState("mr-");
        attributes.setAttribute(ATTRIBUTE, created, RequestAttributes.SCOPE_REQUEST);
        return created;
    }

    synchronized String register(String component) {
        components.add(component);
        return idPrefix + next++;
    }

    synchronized List<String> components() {
        return Collections.unmodifiableList(new ArrayList<>(components));
    }
}
