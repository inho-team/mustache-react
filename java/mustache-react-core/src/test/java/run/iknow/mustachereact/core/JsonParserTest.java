package run.iknow.mustachereact.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonParserTest {

    @Test
    void parsesNestedValues() {
        Object value = JsonParser.parse(" {\"a\": [1, -2.5e1, true, false, null], \"b\": {\"c\": \"x\\\"\\u00e9\\n\"}, \"d\": {}} ");

        assertThat(value).isInstanceOf(Map.class);
        Map<?, ?> map = (Map<?, ?>) value;
        assertThat(map.get("a")).isEqualTo(Arrays.asList(1.0, -25.0, true, false, null));
        assertThat(((Map<?, ?>) map.get("b")).get("c")).isEqualTo("x\"é\n");
        assertThat((Map<?, ?>) map.get("d")).isEmpty();
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> JsonParser.parse("{\"a\": 1,}")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonParser.parse("{\"a\": 1} x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonParser.parse("[1")).isInstanceOf(IllegalArgumentException.class);
    }
}
