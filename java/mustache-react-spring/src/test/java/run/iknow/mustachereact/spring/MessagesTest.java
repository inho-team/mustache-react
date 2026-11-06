package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.samskivert.mustache.Mustache;

class MessagesTest {

    private final ResourceBundleMessageSource source = new ResourceBundleMessageSource();

    MessagesTest() {
        source.setBasename("i18n/shop");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
    }

    private final MessageKeys keys = new MessageKeys(new PathMatchingResourcePatternResolver(),
            Collections.singletonList("classpath:i18n/shop"), StandardCharsets.UTF_8);

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Map<String, Object> contribute(List<String> entries, Locale locale) {
        LocaleContextHolder.setLocale(locale);
        Map<String, Object> context = new HashMap<>();
        new MessagesContextContributor(source, entries, keys::all).contribute(context);
        return context;
    }

    @Test
    void listsTheKeysOfEveryLocaleButNotOtherFiles() {
        assertThat(keys.all()).containsExactly("app.name", "hidden.secret", "orders.count", "orders.only-en",
                "orders.title");
    }

    @Test
    void exposesKeysAndPrefixPatternsInTheRequestLocale() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("app.name", "Shop");
        expected.put("orders.count", "{0} orders");
        expected.put("orders.only-en", "English only");
        expected.put("orders.title", "Orders");
        assertThat(contribute(Arrays.asList("app.name", "orders.*"), Locale.ENGLISH)).containsEntry("messages", expected);

        @SuppressWarnings("unchecked")
        Map<String, String> korean = (Map<String, String>) contribute(Arrays.asList("orders.*"), Locale.KOREAN)
                .get("messages");
        // Keys only in another locale fall back to the key itself, as the client shows missing keys.
        assertThat(korean).containsEntry("orders.title", "주문 목록").containsEntry("orders.count", "주문 {0}건")
                .containsEntry("orders.only-en", "orders.only-en").doesNotContainKey("hidden.secret");
    }

    @Test
    void starExposesEverything() {
        @SuppressWarnings("unchecked")
        Map<String, String> all = (Map<String, String>) contribute(Collections.singletonList("*"), Locale.KOREAN)
                .get("messages");
        assertThat(all)
                .containsKeys("app.name", "hidden.secret", "orders.title");
        assertThat(contribute(Collections.<String>emptyList(), Locale.KOREAN)).doesNotContainKey("messages");
    }

    @Test
    void templatesWriteMessagesWithArgumentsEscaped() {
        Map<String, Object> model = new HashMap<>();
        model.put("mr", new MustacheReactHelpers(null, null, new MessageLambda(source)));
        model.put("count", 1234);
        model.put("name", "<b>");

        LocaleContextHolder.setLocale(Locale.KOREAN);
        String html = Mustache.compiler()
                .compile("<h1>{{#mr.t}}orders.title{{/mr.t}}</h1><p>{{#mr.t}}orders.count count{{/mr.t}}</p>"
                        + "<i>{{#mr.t}}no.such.key{{/mr.t}}</i>")
                .execute(model);
        assertThat(html).isEqualTo("<h1>주문 목록</h1><p>주문 1,234건</p><i>no.such.key</i>");

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(Mustache.compiler().compile("{{#mr.t}}orders.count name{{/mr.t}}").execute(model))
                .isEqualTo("&lt;b&gt; orders");
        assertThatThrownBy(() -> Mustache.compiler().compile("{{#mr.t}}orders.count nope{{/mr.t}}").execute(model))
                .hasMessageContaining("'nope' was not found");
    }

    @Test
    void localeContextNamesTheLanguageSwitch() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ko-KR"));
        Map<String, Object> context = new HashMap<>();
        new LocaleContextContributor("lang", Arrays.asList(Locale.KOREAN, Locale.ENGLISH)).contribute(context);
        assertThat(context).containsEntry("locale", "ko-KR");
        Map<String, Object> i18n = new LinkedHashMap<>();
        i18n.put("param", "lang");
        i18n.put("locales", Arrays.asList("ko", "en"));
        assertThat(context).containsEntry("i18n", i18n);

        Map<String, Object> plain = new HashMap<>();
        new LocaleContextContributor().contribute(plain);
        assertThat(plain).containsOnlyKeys("locale");
    }
}
