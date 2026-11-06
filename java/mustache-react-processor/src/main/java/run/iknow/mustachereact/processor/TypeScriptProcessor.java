package run.iknow.mustachereact.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Writes TypeScript declarations for the props of React pages.
 *
 * <p>Collects the return types of {@code @ReactPage} methods and the classes annotated with {@code @ReactType}, follows
 * the classes they reference, and writes one TypeScript file with an interface per class, a union per enum, and a
 * {@code ReactPages} map from page name to props type:
 *
 * <pre>{@code
 * export interface OrderListResponse { orders: Order[]; total: number; }
 * export interface ReactPages { 'orders/OrderList': OrderListResponse; }
 * }</pre>
 *
 * <p>Options ({@code -A}):
 * <ul>
 *   <li>{@code mustachereact.typesFile}: where to write the file, e.g.
 *       {@code src/main/frontend/generated/types.ts}. Relative paths are resolved against the working directory.
 *       Without it the file is written to the class output as {@code META-INF/mustache-react/types.ts}.</li>
 * </ul>
 *
 * <p>The file is rewritten only when its content changes, so watching tools are not triggered for nothing.
 */
public class TypeScriptProcessor extends AbstractProcessor {

    static final String REACT_PAGE = "run.iknow.mustachereact.spring.ReactPage";
    static final String REACT_TYPE = "run.iknow.mustachereact.spring.ReactType";
    static final String OPTION_TYPES_FILE = "mustachereact.typesFile";
    static final String DEFAULT_RESOURCE = "META-INF/mustache-react/types.ts";

    private final Map<String, TypeMirror> pages = new LinkedHashMap<>();
    private final Map<String, TypeElement> roots = new LinkedHashMap<>();
    private boolean written;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList(REACT_PAGE, REACT_TYPE));
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.singleton(OPTION_TYPES_FILE);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        for (TypeElement annotation : annotations) {
            String name = annotation.getQualifiedName().toString();
            for (Element element : round.getElementsAnnotatedWith(annotation)) {
                if (REACT_PAGE.equals(name) && element.getKind() == ElementKind.METHOD) {
                    collectPage((ExecutableElement) element);
                } else if (REACT_TYPE.equals(name) && element instanceof TypeElement) {
                    TypeElement type = (TypeElement) element;
                    roots.put(type.getQualifiedName().toString(), type);
                }
            }
        }
        if (round.processingOver() && !written && (!pages.isEmpty() || !roots.isEmpty())) {
            written = true;
            write();
        }
        return false;
    }

    private void collectPage(ExecutableElement method) {
        String page = pageName(method);
        if (page == null) {
            return;
        }
        TypeMirror type = unwrapResponseEntity(method.getReturnType());
        TypeMirror previous = pages.get(page);
        // A form handler usually returns Object ("redirect:/..." or the page props again) next to a GET handler that
        // returns the props type: the concrete type wins.
        if (previous != null && isUntyped(type)) {
            return;
        }
        if (type == null || type.getKind() == TypeKind.WILDCARD) {
            type = processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType();
        }
        pages.put(page, type);
        if (previous != null && !isUntyped(previous) && !processingEnv.getTypeUtils().isSameType(previous, type)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "@ReactPage(\"" + page + "\") is used by methods returning different types; the TypeScript "
                            + "props use " + type, method);
        }
    }

    /** {@code ResponseEntity<T>} → {@code T}: the entity's body is the props. */
    private static TypeMirror unwrapResponseEntity(TypeMirror type) {
        if (type instanceof javax.lang.model.type.DeclaredType) {
            javax.lang.model.type.DeclaredType declared = (javax.lang.model.type.DeclaredType) type;
            if (((TypeElement) declared.asElement()).getQualifiedName().contentEquals("org.springframework.http.ResponseEntity")) {
                return declared.getTypeArguments().isEmpty() ? null : declared.getTypeArguments().get(0);
            }
        }
        return type;
    }

    /** {@code void}, {@code Object} and {@code String} ("redirect:...") say nothing about the props. */
    private static boolean isUntyped(TypeMirror type) {
        if (type == null || type.getKind() == TypeKind.WILDCARD) {
            return true;
        }
        String name = type.toString();
        return type.getKind() == TypeKind.VOID || "java.lang.Object".equals(name) || "java.lang.String".equals(name);
    }

    private static String pageName(ExecutableElement method) {
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            if (!REACT_PAGE.equals(((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().toString())) {
                continue;
            }
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                if (entry.getKey().getSimpleName().contentEquals("value")) {
                    return String.valueOf(entry.getValue().getValue());
                }
            }
        }
        return null;
    }

    private void write() {
        TypeScriptWriter writer = new TypeScriptWriter(processingEnv);
        for (TypeElement root : roots.values()) {
            writer.declare(root);
        }
        for (Map.Entry<String, TypeMirror> page : pages.entrySet()) {
            TypeMirror type = page.getValue();
            writer.page(page.getKey(), type.getKind() == TypeKind.VOID ? null : type);
        }
        String content = writer.render();

        String target = processingEnv.getOptions().get(OPTION_TYPES_FILE);
        try {
            if (target != null && !target.trim().isEmpty()) {
                writeIfChanged(Paths.get(target.trim()).toAbsolutePath(), content);
            } else {
                FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", DEFAULT_RESOURCE);
                try (Writer out = file.openWriter()) {
                    out.write(content);
                }
            }
        } catch (IOException ex) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "mustache-react: could not write TypeScript types: " + ex.getMessage());
        }
    }

    private static void writeIfChanged(Path file, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                byte[] existing = readAll(in);
                if (Arrays.equals(existing, bytes)) {
                    return;
                }
            }
        }
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(bytes);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}
