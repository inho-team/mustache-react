package run.iknow.mustachereact.spring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;

import run.iknow.mustachereact.core.Json;
import run.iknow.mustachereact.core.SsrEngine;
import run.iknow.mustachereact.core.SsrException;

/**
 * Renders through the HTTP endpoint of {@code @mustache-react/server}: the SSR sidecar
 * ({@link NodeSsrServer}) or the Vite dev server. {@code POST {component, props, context}} → {@code {html}}.
 */
public class HttpSsrEngine implements SsrEngine {

    /** Path of the endpoint, on the SSR server and on the Vite dev server. */
    public static final String RENDER_PATH = "/__mustache-react/render";

    private final Supplier<String> endpoint;
    private final int timeoutMillis;

    /**
     * @param endpoint the render URL, or {@code null} while the server is not running
     * @param timeoutMillis how long a render may take
     */
    public HttpSsrEngine(Supplier<String> endpoint, int timeoutMillis) {
        this.endpoint = endpoint;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public String render(String component, String propsJson, String contextJson) throws SsrException {
        String url = endpoint.get();
        if (url == null) {
            throw new SsrException("the SSR server is not running");
        }
        byte[] body = ("{\"component\":" + Json.quote(component) + ",\"props\":" + propsJson + ",\"context\":"
                + contextJson + "}").getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(Math.min(timeoutMillis, 1000));
            connection.setReadTimeout(timeoutMillis);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }
            int status = connection.getResponseCode();
            String response = read(status < 400 ? connection.getInputStream() : connection.getErrorStream());
            Object json = response.isEmpty() ? null : Json.parse(response);
            Map<?, ?> result = json instanceof Map ? (Map<?, ?>) json : null;
            if (status == 200 && result != null && result.get("html") instanceof String) {
                return (String) result.get("html");
            }
            Object error = result == null ? null : result.get("error");
            throw new SsrException("rendering " + component + " failed (HTTP " + status + ")"
                    + (error == null ? "" : ": " + error));
        } catch (IOException | IllegalArgumentException ex) {
            throw new SsrException("rendering " + component + " failed: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (InputStream input = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
