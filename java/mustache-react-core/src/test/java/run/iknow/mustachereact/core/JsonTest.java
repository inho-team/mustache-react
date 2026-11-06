package run.iknow.mustachereact.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void quotesStringsSoTheyParseBack() {
        String text = "a\"b\\c\nd\u0001é";

        assertThat(Json.quote(text)).isEqualTo("\"a\\\"b\\\\c\\nd\\u0001é\"");
        assertThat(Json.parse(Json.quote(text))).isEqualTo(text);
    }
}
