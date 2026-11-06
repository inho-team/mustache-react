package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.NativeWebRequest;

class RedirectLocationTest {

    private static NativeWebRequest request(String contextPath) {
        return (NativeWebRequest) Proxy.newProxyInstance(RedirectLocationTest.class.getClassLoader(),
                new Class<?>[] {NativeWebRequest.class},
                (proxy, method, args) -> "getContextPath".equals(method.getName()) ? contextPath : null);
    }

    @Test
    void readsRedirectViewNamesRelativeToTheContextPath() {
        assertThat(ReactPageReturnValueHandler.redirectLocation("redirect:/orders", request("/shop"))).isEqualTo("/shop/orders");
        assertThat(ReactPageReturnValueHandler.redirectLocation("redirect:/orders", request(""))).isEqualTo("/orders");
        assertThat(ReactPageReturnValueHandler.redirectLocation("redirect:https://example.com/x", request("/shop")))
                .isEqualTo("https://example.com/x");
        assertThat(ReactPageReturnValueHandler.redirectLocation("redirect://evil.example/x", request("/shop")))
                .isEqualTo("//evil.example/x");
    }

    @Test
    void ignoresOtherValues() {
        assertThat(ReactPageReturnValueHandler.redirectLocation("orders/list", request(""))).isNull();
        assertThat(ReactPageReturnValueHandler.redirectLocation(new Object(), request(""))).isNull();
        assertThat(ReactPageReturnValueHandler.redirectLocation(null, request(""))).isNull();
    }
}
