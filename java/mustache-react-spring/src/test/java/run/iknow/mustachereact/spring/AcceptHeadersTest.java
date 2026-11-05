package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AcceptHeadersTest {

    @Test
    void browserNavigationGetsHtml() {
        assertThat(AcceptHeaders.prefersHtml(new String[] {
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"})).isTrue();
    }

    @Test
    void apiClientsGetJson() {
        assertThat(AcceptHeaders.prefersHtml(null)).isFalse();
        assertThat(AcceptHeaders.prefersHtml(new String[0])).isFalse();
        assertThat(AcceptHeaders.prefersHtml(new String[] {"*/*"})).isFalse();
        assertThat(AcceptHeaders.prefersHtml(new String[] {"application/json"})).isFalse();
        assertThat(AcceptHeaders.prefersHtml(new String[] {"application/problem+json, text/html;q=0.5"})).isFalse();
        assertThat(AcceptHeaders.prefersHtml(new String[] {"text/html;q=0"})).isFalse();
    }

    @Test
    void htmlWinsTies() {
        assertThat(AcceptHeaders.prefersHtml(new String[] {"application/json", "text/html"})).isTrue();
    }

    @Test
    void ignoresMalformedHeaders() {
        assertThat(AcceptHeaders.prefersHtml(new String[] {"not a media type", "text/html"})).isTrue();
    }
}
