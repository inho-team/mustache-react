package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import run.iknow.mustachereact.core.SsrException;

class HttpSsrEngineTest {

    private HttpServer server;
    private final AtomicReference<String> received = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String response = "{\"html\":\"<p>ok</p>\"}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(HttpSsrEngine.RENDER_PATH, exchange -> {
            received.set(read(exchange.getRequestBody()));
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private HttpSsrEngine engine() {
        return new HttpSsrEngine(() -> "http://127.0.0.1:" + server.getAddress().getPort() + HttpSsrEngine.RENDER_PATH, 2000);
    }

    @Test
    void postsComponentPropsAndContext() throws Exception {
        assertThat(engine().render("pages/orders/OrderList", "{\"total\":1}", "{\"locale\":\"ko\"}")).isEqualTo("<p>ok</p>");
        assertThat(received.get())
                .isEqualTo("{\"component\":\"pages/orders/OrderList\",\"props\":{\"total\":1},\"context\":{\"locale\":\"ko\"}}");
    }

    @Test
    void reportsRenderErrors() {
        status = 500;
        response = "{\"error\":\"ReferenceError: window is not defined\"}";

        assertThatThrownBy(() -> engine().render("islands/Chart", "{}", "{}"))
                .isInstanceOf(SsrException.class)
                .hasMessageContaining("HTTP 500")
                .hasMessageContaining("window is not defined");
    }

    @Test
    void reportsAnUnreachableOrStoppedServer() throws Exception {
        int unused;
        try (ServerSocket socket = new ServerSocket(0)) {
            unused = socket.getLocalPort();
        }
        HttpSsrEngine unreachable = new HttpSsrEngine(() -> "http://127.0.0.1:" + unused + HttpSsrEngine.RENDER_PATH, 500);
        HttpSsrEngine stopped = new HttpSsrEngine(() -> null, 500);

        assertThatThrownBy(() -> unreachable.render("pages/A", "{}", "{}")).isInstanceOf(SsrException.class);
        assertThatThrownBy(() -> stopped.render("pages/A", "{}", "{}")).hasMessageContaining("not running");
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
