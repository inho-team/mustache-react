package run.iknow.mustachereact.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IslandMarkupTest {

    @Test
    void rendersElementAndPropsScript() {
        String html = IslandMarkup.render("pages/orders/OrderList", "mr-0", "{\"q\":\"</script>\"}", null);

        assertThat(html).isEqualTo(
                "<div data-mr-island=\"pages/orders/OrderList\" data-mr-id=\"mr-0\"></div>\n"
                        + "<script type=\"application/json\" data-mr-props=\"mr-0\">{\"q\":\"\\u003c/script\\u003e\"}</script>");
    }

    @Test
    void marksServerRenderedIslands() {
        String html = IslandMarkup.render("islands/Chart", "mr-1", "{}", "<svg></svg>");

        assertThat(html).startsWith("<div data-mr-island=\"islands/Chart\" data-mr-id=\"mr-1\" data-mr-ssr=\"true\"><svg></svg></div>");
    }

    @Test
    void rejectsNamesThatAreNotRelativePaths() {
        for (String name : new String[] {"", "/abs", "a//b", "../x", "a/../b", "a\"b", "a b", "a<b"}) {
            assertThatThrownBy(() -> IslandMarkup.requireValidName(name))
                    .as(name)
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(IslandMarkup.requireValidName("pages/orders/Order-List_2")).isEqualTo("pages/orders/Order-List_2");
    }
}
