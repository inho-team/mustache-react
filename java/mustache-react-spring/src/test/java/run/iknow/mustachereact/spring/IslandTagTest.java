package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IslandTagTest {

    @Test
    void parsesNameAndProps() {
        IslandTag tag = IslandTag.parse("  charts/OrderChart\n  orders\n  user=currentUser id=order.id item=this ");

        assertThat(tag.component).isEqualTo("charts/OrderChart");
        assertThat(tag.props).containsExactly(
                entry("orders", "orders"), entry("user", "currentUser"), entry("id", "order.id"), entry("item", "this"));
    }

    @Test
    void explainsMistakes() {
        assertThatThrownBy(() -> IslandTag.parse("  ")).hasMessageContaining("Empty {{#react}} section");
        assertThatThrownBy(() -> IslandTag.parse("../Secret")).hasMessageContaining("Invalid component name");
        assertThatThrownBy(() -> IslandTag.parse("Chart a=")).hasMessageContaining("Invalid prop 'a='");
        assertThatThrownBy(() -> IslandTag.parse("Chart 1x")).hasMessageContaining("Invalid prop '1x'");
        assertThatThrownBy(() -> IslandTag.parse("Chart a=b..c")).hasMessageContaining("Invalid prop");
        assertThatThrownBy(() -> IslandTag.parse("Chart this")).hasMessageContaining("Invalid prop 'this'");
        assertThatThrownBy(() -> IslandTag.parse("Chart a a=b")).hasMessageContaining("given twice");
    }

    private static java.util.Map.Entry<String, String> entry(String key, String value) {
        return new java.util.AbstractMap.SimpleEntry<>(key, value);
    }
}
