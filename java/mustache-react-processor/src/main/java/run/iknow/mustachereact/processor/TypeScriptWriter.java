package run.iknow.mustachereact.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

/**
 * Maps Java types to TypeScript the way Spring Boot's default Jackson setup writes them as JSON.
 *
 * <ul>
 *   <li>numbers ({@code int}, {@code long}, {@code BigDecimal}, ...) → {@code number}</li>
 *   <li>{@code String}, {@code UUID}, {@code java.time} types, {@code Date} → {@code string}
 *       (Spring Boot writes dates as ISO-8601 strings)</li>
 *   <li>collections and arrays → {@code T[]}; {@code byte[]} → {@code string} (Base64); maps → {@code Record<string, V>}</li>
 *   <li>{@code Optional<T>} and members annotated {@code @Nullable} → {@code T | null}</li>
 *   <li>enums → a union of their names; other classes, records and interfaces → an interface of their JSON
 *       properties (getters, record components, public fields, Lombok {@code @Getter}/{@code @Data}/{@code @Value}
 *       fields), honouring {@code @JsonIgnore}, {@code @JsonProperty} and {@code @JsonIgnoreProperties}</li>
 * </ul>
 */
final class TypeScriptWriter {

    private static final String JSON_IGNORE = "com.fasterxml.jackson.annotation.JsonIgnore";
    private static final String JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty";
    private static final String JSON_IGNORE_PROPERTIES = "com.fasterxml.jackson.annotation.JsonIgnoreProperties";
    private static final Set<String> LOMBOK_CLASS_GETTERS =
            new HashSet<>(Arrays.asList("lombok.Data", "lombok.Getter", "lombok.Value"));

    private static final Set<String> STRINGS = new HashSet<>(Arrays.asList(
            "java.lang.String", "java.lang.CharSequence", "java.lang.Character", "java.util.UUID", "java.net.URI",
            "java.net.URL", "java.util.Locale", "java.util.Currency", "java.time.ZoneId", "java.time.ZoneOffset",
            "java.util.TimeZone", "java.util.Date", "java.util.Calendar", "java.sql.Date", "java.sql.Time",
            "java.sql.Timestamp", "java.time.Instant", "java.time.LocalDate", "java.time.LocalDateTime",
            "java.time.LocalTime", "java.time.OffsetDateTime", "java.time.OffsetTime", "java.time.ZonedDateTime",
            "java.time.Year", "java.time.YearMonth", "java.time.MonthDay", "java.time.Duration", "java.time.Period",
            "java.lang.Class"));
    private static final Set<String> NUMBERS = new HashSet<>(Arrays.asList(
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float",
            "java.lang.Double", "java.lang.Number", "java.math.BigDecimal", "java.math.BigInteger",
            "java.util.concurrent.atomic.AtomicInteger", "java.util.concurrent.atomic.AtomicLong",
            "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble"));
    private static final Set<String> UNKNOWN = new HashSet<>(Arrays.asList(
            "java.lang.Object", "com.fasterxml.jackson.databind.JsonNode", "tools.jackson.databind.JsonNode"));

    private final Types types;

    /** Qualified class name → TypeScript name. */
    private final Map<String, String> names = new HashMap<>();
    private final Set<String> usedNames = new HashSet<>();
    /** TypeScript name → declaration. */
    private final Map<String, String> declarations = new TreeMap<>();
    /** Page name → props type. */
    private final Map<String, String> pages = new TreeMap<>();
    /** Island name → props type. */
    private final Map<String, String> islands = new TreeMap<>();

    TypeScriptWriter(ProcessingEnvironment env) {
        this.types = env.getTypeUtils();
    }

    void declare(TypeElement element) {
        declaredName(element);
    }

    void page(String page, TypeMirror props) {
        pages.put(page, props == null ? "Record<string, never>" : type(props));
    }

    /**
     * Declares the props type of an island.
     *
     * @return the JSON property names, nullable ones followed by {@code ?}
     */
    List<String> island(String island, TypeElement props) {
        islands.put(island, declaredName(props));
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, String> property : properties(props).entrySet()) {
            names.add(property.getKey() + (property.getValue().endsWith(" | null") ? "?" : ""));
        }
        return names;
    }

    String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by mustache-react-processor from the Java classes. Do not edit.\n");
        sb.append("// Rebuild the Java project to update it.\n\n");
        for (String declaration : declarations.values()) {
            sb.append(declaration).append('\n');
        }
        sb.append("/** Props of each @ReactPage, by page name. */\n");
        sb.append("export interface ReactPages {\n");
        for (Map.Entry<String, String> page : pages.entrySet()) {
            sb.append("  '").append(page.getKey().replace("\\", "\\\\").replace("'", "\\'")).append("': ")
                    .append(page.getValue()).append(";\n");
        }
        sb.append("}\n\n");
        sb.append("/** Props of a page, e.g. `PageProps<'orders/OrderList'>`. */\n");
        sb.append("export type PageProps<P extends keyof ReactPages> = ReactPages[P];\n");
        if (!islands.isEmpty()) {
            sb.append("\n/** Props of each @ReactIsland, by island name. */\n");
            sb.append("export interface ReactIslands {\n");
            for (Map.Entry<String, String> island : islands.entrySet()) {
                sb.append("  '").append(island.getKey().replace("\\", "\\\\").replace("'", "\\'")).append("': ")
                        .append(island.getValue()).append(";\n");
            }
            sb.append("}\n\n");
            sb.append("/** Props of an island, e.g. `IslandProps<'StatusBadge'>`. */\n");
            sb.append("export type IslandProps<I extends keyof ReactIslands> = ReactIslands[I];\n");
        }
        return sb.toString();
    }

    // --- Types --------------------------------------------------------------------------------------------------

    String type(TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN:
                return "boolean";
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
                return "number";
            case CHAR:
                return "string";
            case ARRAY:
                TypeMirror component = ((ArrayType) type).getComponentType();
                return component.getKind() == TypeKind.BYTE ? "string" : arrayOf(type(component));
            case TYPEVAR:
                return type.toString().replaceAll(".*\\.", "");
            case WILDCARD:
                TypeMirror bound = ((WildcardType) type).getExtendsBound();
                return bound == null ? "unknown" : type(bound);
            case DECLARED:
                return declared((DeclaredType) type);
            default:
                return "unknown";
        }
    }

    private String declared(DeclaredType type) {
        TypeElement element = (TypeElement) type.asElement();
        String name = element.getQualifiedName().toString();
        List<? extends TypeMirror> args = type.getTypeArguments();

        if (STRINGS.contains(name)) {
            return "string";
        }
        if (NUMBERS.contains(name)) {
            return "number";
        }
        if ("java.lang.Boolean".equals(name) || "java.util.concurrent.atomic.AtomicBoolean".equals(name)) {
            return "boolean";
        }
        if (UNKNOWN.contains(name)) {
            return "unknown";
        }
        if ("java.util.Optional".equals(name)) {
            return nullable(args.isEmpty() ? "unknown" : type(args.get(0)));
        }
        if (isSubtypeOfErasure(type, "java.util.Map")) {
            TypeMirror value = typeArgument(type, "java.util.Map", 1);
            return "Record<string, " + (value == null ? "unknown" : type(value)) + ">";
        }
        if (isSubtypeOfErasure(type, "java.lang.Iterable") || isSubtypeOfErasure(type, "java.util.stream.Stream")) {
            String iterable = isSubtypeOfErasure(type, "java.lang.Iterable") ? "java.lang.Iterable" : "java.util.stream.Stream";
            TypeMirror item = typeArgument(type, iterable, 0);
            return arrayOf(item == null ? "unknown" : type(item));
        }
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jakarta.")) {
            return "unknown";
        }

        String tsName = declaredName(element);
        if (args.isEmpty() || element.getKind() == ElementKind.ENUM) {
            return tsName;
        }
        List<String> tsArgs = new ArrayList<>();
        for (TypeMirror arg : args) {
            tsArgs.add(type(arg));
        }
        return tsName + "<" + String.join(", ", tsArgs) + ">";
    }

    /**
     * Whether {@code type} is {@code qualifiedName} or extends or implements it. Walks the supertypes by name instead
     * of using {@code Types.isSubtype}, whose result on erased types differs between javac versions.
     */
    private boolean isSubtypeOfErasure(TypeMirror type, String qualifiedName) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        if (((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().contentEquals(qualifiedName)) {
            return true;
        }
        for (TypeMirror supertype : types.directSupertypes(type)) {
            if (isSubtypeOfErasure(supertype, qualifiedName)) {
                return true;
            }
        }
        return false;
    }

    /** The {@code index}-th type argument of {@code type} seen as {@code supertypeName}, e.g. the V of a Map subtype. */
    private TypeMirror typeArgument(TypeMirror type, String supertypeName, int index) {
        if (!(type instanceof DeclaredType)) {
            return null;
        }
        DeclaredType declared = (DeclaredType) type;
        if (((TypeElement) declared.asElement()).getQualifiedName().contentEquals(supertypeName)) {
            List<? extends TypeMirror> args = declared.getTypeArguments();
            return args.size() > index ? args.get(index) : null;
        }
        for (TypeMirror supertype : types.directSupertypes(type)) {
            if (isSubtypeOfErasure(supertype, supertypeName)) {
                return typeArgument(supertype, supertypeName, index);
            }
        }
        return null;
    }

    private static String arrayOf(String item) {
        return (item.contains(" ") ? "(" + item + ")" : item) + "[]";
    }

    private static String nullable(String type) {
        return type.endsWith(" | null") ? type : type + " | null";
    }

    // --- Declarations -------------------------------------------------------------------------------------------

    private String declaredName(TypeElement element) {
        String qualified = element.getQualifiedName().toString();
        String existing = names.get(qualified);
        if (existing != null) {
            return existing;
        }
        String name = uniqueName(element);
        names.put(qualified, name);
        // Registered before the members are visited, so self-referencing classes terminate.
        declarations.put(name, element.getKind() == ElementKind.ENUM ? enumDeclaration(name, element)
                : interfaceDeclaration(name, element));
        return name;
    }

    private String uniqueName(TypeElement element) {
        String simple = element.getSimpleName().toString();
        if (usedNames.add(simple)) {
            return simple;
        }
        StringBuilder outer = new StringBuilder(simple);
        Element enclosing = element.getEnclosingElement();
        while (enclosing instanceof TypeElement) {
            outer.insert(0, enclosing.getSimpleName());
            enclosing = enclosing.getEnclosingElement();
        }
        if (usedNames.add(outer.toString())) {
            return outer.toString();
        }
        String packageName = enclosing instanceof PackageElement
                ? ((PackageElement) enclosing).getQualifiedName().toString() : "";
        String candidate = packageName.replaceAll("[^A-Za-z0-9]+", "_") + "_" + outer;
        int suffix = 2;
        String unique = candidate;
        while (!usedNames.add(unique)) {
            unique = candidate + suffix++;
        }
        return unique;
    }

    private String enumDeclaration(String name, TypeElement element) {
        List<String> constants = new ArrayList<>();
        for (Element member : element.getEnclosedElements()) {
            if (member.getKind() == ElementKind.ENUM_CONSTANT && !hasAnnotation(member, JSON_IGNORE)) {
                String json = annotationString(member, JSON_PROPERTY, "value");
                constants.add("'" + (json == null || json.isEmpty() ? member.getSimpleName().toString() : json) + "'");
            }
        }
        return "export type " + name + " = " + (constants.isEmpty() ? "never" : String.join(" | ", constants)) + ";\n";
    }

    private String interfaceDeclaration(String name, TypeElement element) {
        StringBuilder sb = new StringBuilder("export interface ").append(name);
        List<? extends TypeParameterElement> parameters = element.getTypeParameters();
        if (!parameters.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (TypeParameterElement parameter : parameters) {
                names.add(parameter.getSimpleName().toString());
            }
            sb.append('<').append(String.join(", ", names)).append('>');
        }
        sb.append(" {\n");
        for (Map.Entry<String, String> property : properties(element).entrySet()) {
            sb.append("  ").append(propertyKey(property.getKey())).append(": ").append(property.getValue()).append(";\n");
        }
        return sb.append("}\n").toString();
    }

    private static String propertyKey(String name) {
        return name.matches("[A-Za-z_$][A-Za-z0-9_$]*") ? name : "'" + name.replace("'", "\\'") + "'";
    }

    /** JSON property name → TypeScript type, for a class and its superclasses. */
    private Map<String, String> properties(TypeElement element) {
        DeclaredType self = (DeclaredType) element.asType();
        Set<String> ignored = new HashSet<>(annotationStrings(element, JSON_IGNORE_PROPERTIES, "value"));
        Map<String, Member> members = new LinkedHashMap<>();

        boolean lombokClass = false;
        for (TypeElement current = element; current != null; current = superclass(current)) {
            for (String annotation : LOMBOK_CLASS_GETTERS) {
                lombokClass |= hasAnnotation(current, annotation);
            }
            collectMembers(current, members, lombokClass);
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Member> entry : members.entrySet()) {
            Member member = entry.getValue();
            if (member.ignored || ignored.contains(entry.getKey())) {
                continue;
            }
            TypeMirror memberType = member.element.getKind() == ElementKind.METHOD
                    ? ((javax.lang.model.type.ExecutableType) types.asMemberOf(self, member.element)).getReturnType()
                    : types.asMemberOf(self, member.element);
            String tsType = type(memberType);
            result.put(member.jsonName, member.nullable ? nullable(tsType) : tsType);
        }
        return result;
    }

    private TypeElement superclass(TypeElement element) {
        TypeMirror superclass = element.getSuperclass();
        if (superclass.getKind() != TypeKind.DECLARED) {
            return null;
        }
        TypeElement parent = (TypeElement) ((DeclaredType) superclass).asElement();
        String name = parent.getQualifiedName().toString();
        return name.startsWith("java.") ? null : parent;
    }

    private static final class Member {
        final Element element;
        final String javaName;
        final String jsonName;
        final boolean ignored;
        final boolean nullable;

        Member(Element element, String javaName, String jsonName, boolean ignored, boolean nullable) {
            this.element = element;
            this.javaName = javaName;
            this.jsonName = jsonName;
            this.ignored = ignored;
            this.nullable = nullable;
        }
    }

    /**
     * Adds the JSON properties declared by {@code type}. Members already found (in a subclass) win. Annotations
     * on a property's field and getter are combined, as Jackson does.
     */
    private void collectMembers(TypeElement type, Map<String, Member> members, boolean lombokClass) {
        Map<String, VariableElement> fields = new LinkedHashMap<>();
        for (Element member : type.getEnclosedElements()) {
            if (member.getKind() == ElementKind.FIELD && !member.getModifiers().contains(Modifier.STATIC)) {
                fields.put(member.getSimpleName().toString(), (VariableElement) member);
            }
        }
        boolean record = "RECORD".equals(type.getKind().name());
        Set<String> recordComponents = new HashSet<>();
        if (record) {
            for (Element member : type.getEnclosedElements()) {
                if ("RECORD_COMPONENT".equals(member.getKind().name())) {
                    recordComponents.add(member.getSimpleName().toString());
                }
            }
        }
        boolean isInterface = type.getKind() == ElementKind.INTERFACE;

        for (Element member : type.getEnclosedElements()) {
            if (member.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) member;
            Set<Modifier> modifiers = method.getModifiers();
            if (modifiers.contains(Modifier.STATIC) || !method.getParameters().isEmpty()
                    || method.getReturnType().getKind() == TypeKind.VOID
                    || (!isInterface && !modifiers.contains(Modifier.PUBLIC))) {
                continue;
            }
            String methodName = method.getSimpleName().toString();
            String property;
            if (record && recordComponents.contains(methodName)) {
                property = methodName;
            } else if (methodName.startsWith("get") && methodName.length() > 3) {
                property = jacksonName(methodName.substring(3));
            } else if (methodName.startsWith("is") && methodName.length() > 2 && isBoolean(method.getReturnType())) {
                property = jacksonName(methodName.substring(2));
            } else {
                continue;
            }
            VariableElement field = fields.get(property);
            String json = jsonName(property, method, field);
            if (members.containsKey(json) || "class".equals(property)) {
                continue;
            }
            members.put(json, new Member(method, property, json, isIgnored(method, field), isNullable(method, field)));
        }

        for (VariableElement field : fields.values()) {
            String property = field.getSimpleName().toString();
            boolean visible = field.getModifiers().contains(Modifier.PUBLIC)
                    || lombokClass || hasAnnotation(field, "lombok.Getter");
            String json = jsonName(property, null, field);
            if (!visible || members.containsKey(json) || hasJavaProperty(members, property)) {
                continue;
            }
            members.put(json, new Member(field, property, json, isIgnored(null, field), isNullable(null, field)));
        }
    }

    private static boolean hasJavaProperty(Map<String, Member> members, String property) {
        for (Member member : members.values()) {
            if (member.javaName.equals(property)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBoolean(TypeMirror type) {
        return type.getKind() == TypeKind.BOOLEAN || "java.lang.Boolean".equals(type.toString());
    }

    /** Jackson's default property naming: the leading run of capitals is lower-cased ({@code getURL} → {@code url}). */
    static String jacksonName(String suffix) {
        StringBuilder sb = new StringBuilder(suffix.length());
        int i = 0;
        while (i < suffix.length() && Character.isUpperCase(suffix.charAt(i))) {
            sb.append(Character.toLowerCase(suffix.charAt(i)));
            i++;
        }
        return sb.append(suffix, i, suffix.length()).toString();
    }

    private String jsonName(String property, Element getter, Element field) {
        for (Element element : new Element[] {getter, field}) {
            if (element != null) {
                String json = annotationString(element, JSON_PROPERTY, "value");
                if (json != null && !json.isEmpty()) {
                    return json;
                }
            }
        }
        return property;
    }

    private boolean isIgnored(Element getter, Element field) {
        for (Element element : new Element[] {getter, field}) {
            if (element != null && hasAnnotation(element, JSON_IGNORE)) {
                String value = annotationString(element, JSON_IGNORE, "value");
                if (value == null || Boolean.parseBoolean(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNullable(Element getter, Element field) {
        for (Element element : new Element[] {getter, field}) {
            if (element == null) {
                continue;
            }
            for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                if (mirror.getAnnotationType().asElement().getSimpleName().contentEquals("Nullable")) {
                    return true;
                }
            }
            TypeMirror type = element.getKind() == ElementKind.METHOD
                    ? ((ExecutableElement) element).getReturnType() : element.asType();
            for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
                if (mirror.getAnnotationType().asElement().getSimpleName().contentEquals("Nullable")) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Annotations ----------------------------------------------------------------------------------------------

    private static AnnotationMirror annotation(Element element, String qualifiedName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().contentEquals(qualifiedName)) {
                return mirror;
            }
        }
        return null;
    }

    private static boolean hasAnnotation(Element element, String qualifiedName) {
        return annotation(element, qualifiedName) != null;
    }

    private static String annotationString(Element element, String qualifiedName, String attribute) {
        AnnotationMirror mirror = annotation(element, qualifiedName);
        if (mirror == null) {
            return null;
        }
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(attribute)) {
                return String.valueOf(entry.getValue().getValue());
            }
        }
        return null;
    }

    private static List<String> annotationStrings(Element element, String qualifiedName, String attribute) {
        AnnotationMirror mirror = annotation(element, qualifiedName);
        List<String> values = new ArrayList<>();
        if (mirror == null) {
            return values;
        }
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
            if (!entry.getKey().getSimpleName().contentEquals(attribute)) {
                continue;
            }
            Object value = entry.getValue().getValue();
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    values.add(String.valueOf(((AnnotationValue) item).getValue()));
                }
            } else {
                values.add(String.valueOf(value));
            }
        }
        return values;
    }
}
