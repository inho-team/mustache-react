package run.iknow.mustachereact.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlTest {

    @Test
    void escapesMarkupCharacters() {
        assertThat(Html.escape("<a href=\"x\">'&'</a>"))
                .isEqualTo("&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;");
        assertThat(Html.escape(null)).isEmpty();
    }

    @Test
    void scriptSafeJsonCannotCloseTheScriptElement() {
        String json = "{\"text\":\"</script><!-- & \u2028\u2029\"}";

        String safe = Html.scriptSafeJson(json);

        assertThat(safe)
                .doesNotContain("<", ">", "&", "\u2028", "\u2029")
                .isEqualTo("{\"text\":\"\\u003c/script\\u003e\\u003c!-- \\u0026 \\u2028\\u2029\"}");
        // Still the same JSON value.
        assertThat(JsonParser.parse(safe)).isEqualTo(JsonParser.parse(json));
    }

    @Test
    void leavesSafeJsonUntouched() {
        String json = "{\"a\":[1,2]}";
        assertThat(Html.scriptSafeJson(json)).isSameAs(json);
    }
}
