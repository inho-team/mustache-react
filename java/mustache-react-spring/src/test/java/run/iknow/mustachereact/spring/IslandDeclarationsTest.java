package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;

class IslandDeclarationsTest {

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private IslandDeclarations load(String content) throws IOException {
        Path file = dir.resolve(IslandDeclarations.RESOURCE);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        try (URLClassLoader loader = new URLClassLoader(new URL[] {dir.toUri().toURL()}, null)) {
            return IslandDeclarations.load(loader);
        }
    }

    @Test
    void acceptsDeclaredPropsAndOptionalOnesMayBeLeftOut() throws IOException {
        IslandDeclarations declarations = load("StatusBadge=status,orderId,note?\ncharts/OrderChart=values\n");

        assertThat(declarations.islands()).containsExactlyInAnyOrder("StatusBadge", "charts/OrderChart");
        declarations.check("StatusBadge", new HashSet<>(Arrays.asList("status", "orderId")));
        declarations.check("StatusBadge", new HashSet<>(Arrays.asList("status", "orderId", "note")));
        // Undeclared islands accept anything.
        declarations.check("Other", Collections.singleton("whatever"));
    }

    @Test
    void reportsUnknownAndMissingProps() throws IOException {
        IslandDeclarations declarations = load("StatusBadge=status,orderId,note?\n");

        assertThatThrownBy(() -> declarations.check("StatusBadge", new HashSet<>(Arrays.asList("statsu", "orderId"))))
                .hasMessage("island StatusBadge: unknown prop 'statsu', missing prop 'status'. "
                        + "Its @ReactIsland props are status, orderId, note (optional).");
    }

    @Test
    void theRendererChecksEveryIsland() throws IOException {
        RequestContextHolder.setRequestAttributes(new MapRequestAttributes());
        IslandRenderer renderer = new IslandRenderer(new PropsSerializer(
                Collections.<HttpMessageConverter<?>>singletonList(new ReactIslandLambdaTest.MapJsonConverter())),
                ServerSideRendering.off(), null, null).declarations(load("Badge=status\n"));
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("status", "PAID");

        assertThat(renderer.render("Badge", props)).contains("{\"status\":\"PAID\"}");
        props.put("extra", 1);
        assertThatThrownBy(() -> renderer.render("Badge", props)).hasMessageContaining("unknown prop 'extra'");
    }
}
