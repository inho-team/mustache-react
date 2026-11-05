package run.iknow.mustachereact.spring;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.samskivert.mustache.Template;

/**
 * Looks up values such as {@code orders} or {@code order.customer} the way Mustache does: the first segment is
 * searched from the innermost section (the current loop item) outward to the model, the following segments are
 * read from the value found.
 *
 * <p>A segment is read as a {@link Map} key, a method {@code name()} (records), a getter {@code getName()} or
 * {@code isName()}, or a public field, in that order.
 */
final class ContextPaths {

    /** Marks a path that does not exist, as opposed to one whose value is {@code null}. */
    static final Object MISSING = new Object() {
        @Override
        public String toString() {
            return "MISSING";
        }
    };

    /** Path segment for the current context, as in Mustache. */
    static final String THIS = "this";

    private static final int MAX_DEPTH = 256;

    private ContextPaths() {
    }

    static Object resolve(Template.Fragment fragment, String path) {
        String[] segments = path.split("\\.", -1);
        Object value;
        if (THIS.equals(segments[0])) {
            value = fragment.context();
        } else {
            value = MISSING;
            for (int depth = 0; depth < MAX_DEPTH; depth++) {
                Object context;
                try {
                    context = fragment.context(depth);
                } catch (NullPointerException ex) {
                    break; // JMustache signals the end of the context stack this way
                }
                Object candidate = property(context, segments[0]);
                if (candidate != MISSING) {
                    value = candidate;
                    break;
                }
            }
        }
        for (int i = 1; i < segments.length && value != MISSING; i++) {
            value = value == null ? MISSING : property(value, segments[i]);
        }
        return value;
    }

    static Object property(Object target, String name) {
        if (target == null) {
            return MISSING;
        }
        if (target instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) target;
            return map.containsKey(name) ? map.get(name) : MISSING;
        }
        Class<?> type = target.getClass();
        String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (String methodName : new String[] {name, "get" + capitalized, "is" + capitalized}) {
            Method method = publicMethod(type, methodName);
            if (method != null) {
                try {
                    return method.invoke(target);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("Could not read '" + name + "' of " + type.getName(), ex);
                }
            }
        }
        try {
            Field field = type.getField(name);
            if (!Modifier.isStatic(field.getModifiers())) {
                return field.get(target);
            }
        } catch (NoSuchFieldException ex) {
            // fall through
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not read '" + name + "' of " + type.getName(), ex);
        }
        return MISSING;
    }

    private static Method publicMethod(Class<?> type, String name) {
        Method method;
        try {
            method = type.getMethod(name);
        } catch (NoSuchMethodException ex) {
            return null;
        }
        if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() == void.class
                || method.getDeclaringClass() == Object.class) {
            return null;
        }
        if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            // Public method of a non-public class (e.g. a private DTO): allowed when reflection permits it.
            try {
                method.setAccessible(true);
            } catch (RuntimeException ex) {
                return null;
            }
        }
        return method;
    }
}
