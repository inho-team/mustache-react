package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class YamlMessageSourceTest {

    private final YamlMessageSource source = new YamlMessageSource(new PathMatchingResourcePatternResolver(),
            Arrays.asList("yamlmsg/shop", "classpath:yamlmsg/extra"));

    @Test
    void readsNestedKeysPerLocaleLikeResourceBundles() {
        assertThat(source.getMessage("orders.title", null, Locale.KOREAN)).isEqualTo("주문 목록");
        assertThat(source.getMessage("orders.title", null, Locale.ENGLISH)).isEqualTo("Orders");
        assertThat(source.getMessage("orders.title", null, Locale.US)).isEqualTo("Orders (US)");
        // Falls back to the less specific files, then the default one.
        assertThat(source.getMessage("orders.count", new Object[] {3}, Locale.US)).isEqualTo("3 orders");
        assertThat(source.getMessage("app.name", null, Locale.ENGLISH)).isEqualTo("가게");
        assertThat(source.getMessage("missing", null, "default", Locale.KOREAN)).isEqualTo("default");
    }

    @Test
    void formatsLikeSpringsOtherMessageSources() {
        assertThat(source.getMessage("orders.count", new Object[] {1234}, Locale.KOREAN)).isEqualTo("주문 1,234건");
        assertThat(source.getMessage("orders.quote", new Object[] {"ok"}, Locale.KOREAN)).isEqualTo("It's ok");
        assertThat(source.getMessage("orders.quote", null, Locale.KOREAN)).isEqualTo("It''s {0}");
        assertThat(source.getMessage("orders.limit", null, Locale.KOREAN)).isEqualTo("5");
        assertThat(source.getMessage("orders.steps[1]", null, Locale.KOREAN)).isEqualTo("결제");
    }

    @Test
    void earlierBaseNamesWin() {
        assertThat(source.getMessage("app.name", null, Locale.KOREAN)).isEqualTo("가게");
        assertThat(source.getMessage("app.tagline", null, Locale.KOREAN)).isEqualTo("예제");
    }

    @Test
    void readsEditedFilesWhenNotCached(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("live.yml");
        Files.write(file, "a: one\n".getBytes(StandardCharsets.UTF_8));
        try (java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {dir.toUri().toURL()}, null)) {
            YamlMessageSource live = new YamlMessageSource(new PathMatchingResourcePatternResolver(loader),
                    Collections.singletonList("live"));
            live.setCacheMillis(0);
            assertThat(live.getMessage("a", null, Locale.KOREAN)).isEqualTo("one");
            Files.write(file, "a: two\nb: new\n".getBytes(StandardCharsets.UTF_8));
            assertThat(live.getMessage("a", null, Locale.KOREAN)).isEqualTo("two");
            assertThat(live.getMessage("b", null, Locale.KOREAN)).isEqualTo("new");
        }
    }

    @Test
    void existsOnlyWithYamlFiles() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        assertThat(YamlMessageSource.exists(resolver, Collections.singletonList("yamlmsg/shop"))).isTrue();
        assertThat(YamlMessageSource.exists(resolver, Collections.singletonList("yamlmsg/extra"))).isTrue();
        assertThat(YamlMessageSource.exists(resolver, Collections.singletonList("i18n/shop"))).isFalse();
    }

    @Test
    void messageKeysListYamlKeysForPatterns() {
        MessageKeys keys = new MessageKeys(new PathMatchingResourcePatternResolver(),
                Collections.singletonList("yamlmsg/shop"), StandardCharsets.UTF_8);
        assertThat(keys.all()).contains("app.name", "orders.title", "orders.count", "orders.limit", "orders.steps[0]");
    }
}
