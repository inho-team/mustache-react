package run.iknow.mustachereact.spring;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.context.request.RequestAttributes;

/** Request attributes without a servlet request, for tests. */
class MapRequestAttributes implements RequestAttributes {

    private final Map<String, Object> attributes = new HashMap<>();

    @Override
    public Object getAttribute(String name, int scope) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name, int scope) {
        attributes.remove(name);
    }

    @Override
    public String[] getAttributeNames(int scope) {
        return attributes.keySet().toArray(new String[0]);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback, int scope) {
    }

    @Override
    public Object resolveReference(String key) {
        return null;
    }

    @Override
    public String getSessionId() {
        return "test";
    }

    @Override
    public Object getSessionMutex() {
        return this;
    }
}
