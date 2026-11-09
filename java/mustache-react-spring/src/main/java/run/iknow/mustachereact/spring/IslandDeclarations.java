package run.iknow.mustachereact.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The props of each {@link ReactIsland}, as {@code mustache-react-processor} recorded them in
 * {@code META-INF/mustache-react/islands.properties}, to check what templates pass to islands.
 */
public final class IslandDeclarations {

    /** Written by {@code mustache-react-processor}: {@code StatusBadge=status,orderId,note?}. */
    public static final String RESOURCE = "META-INF/mustache-react/islands.properties";

    private final Map<String, Declaration> islands;

    private IslandDeclarations(Map<String, Declaration> islands) {
        this.islands = islands;
    }

    /** No declarations: every island accepts any props. */
    public static IslandDeclarations none() {
        return new IslandDeclarations(Collections.<String, Declaration>emptyMap());
    }

    /**
     * @param classLoader where to find the resources of the application and its libraries
     * @return the declarations of all {@code islands.properties} on the class path
     */
    public static IslandDeclarations load(ClassLoader classLoader) {
        Map<String, Declaration> islands = new LinkedHashMap<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(RESOURCE);
            while (resources.hasMoreElements()) {
                Properties properties = new Properties();
                try (InputStream in = resources.nextElement().openStream()) {
                    properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
                for (String island : properties.stringPropertyNames()) {
                    islands.put(island, Declaration.parse(properties.getProperty(island)));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("mustache-react: could not read " + RESOURCE, e);
        }
        return new IslandDeclarations(islands);
    }

    /** @return the declared island names */
    public Set<String> islands() {
        return Collections.unmodifiableSet(islands.keySet());
    }

    /**
     * @param island island name, e.g. {@code StatusBadge}
     * @param props the props a template passes
     * @throws IllegalArgumentException if the island is declared and a prop is unknown or a required one is missing
     */
    public void check(String island, Set<String> props) {
        Declaration declaration = islands.get(island);
        if (declaration == null) {
            return;
        }
        List<String> problems = new ArrayList<>();
        for (String prop : props) {
            if (!declaration.all.contains(prop)) {
                problems.add("unknown prop '" + prop + "'");
            }
        }
        for (String required : declaration.required) {
            if (!props.contains(required)) {
                problems.add("missing prop '" + required + "'");
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("island " + island + ": " + String.join(", ", problems)
                    + ". Its @ReactIsland props are " + declaration.describe() + ".");
        }
    }

    private static final class Declaration {
        final Set<String> all = new LinkedHashSet<>();
        final Set<String> required = new LinkedHashSet<>();

        static Declaration parse(String value) {
            Declaration declaration = new Declaration();
            for (String entry : value.split(",")) {
                String name = entry.trim();
                if (name.isEmpty()) {
                    continue;
                }
                boolean nullable = name.endsWith("?");
                name = nullable ? name.substring(0, name.length() - 1) : name;
                declaration.all.add(name);
                if (!nullable) {
                    declaration.required.add(name);
                }
            }
            return declaration;
        }

        String describe() {
            List<String> names = new ArrayList<>();
            for (String name : all) {
                names.add(required.contains(name) ? name : name + " (optional)");
            }
            return names.isEmpty() ? "none" : String.join(", ", names);
        }
    }
}
