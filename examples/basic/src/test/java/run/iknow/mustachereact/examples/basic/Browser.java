package run.iknow.mustachereact.examples.basic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal HTTP client with a cookie jar and no automatic redirects, for tests that sign in. Works on Java 8 and
 * with every supported Spring Boot version.
 */
final class Browser {

    static final class Response {
        final int status;
        final String body;
        final Map<String, List<String>> headers;

        Response(int status, String body, Map<String, List<String>> headers) {
            this.status = status;
            this.body = body;
            this.headers = headers;
        }

        String header(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (name.equalsIgnoreCase(entry.getKey())) {
                    return entry.getValue().get(0);
                }
            }
            return null;
        }

        /** The CSRF token of the server context written into the page. */
        String csrfToken() {
            Matcher matcher = Pattern.compile("\"csrf\":\\{[^}]*\"token\":\"([^\"]+)\"").matcher(body);
            if (!matcher.find()) {
                throw new AssertionError("No CSRF token in the server context:\n" + body);
            }
            return matcher.group(1);
        }
    }

    private final String base;
    private final Map<String, String> cookies = new LinkedHashMap<>();

    Browser(String base) {
        this.base = base;
    }

    Response get(String path, String accept) throws IOException {
        return send("GET", path, accept, null, null, null);
    }

    Response postForm(String path, Map<String, String> fields) throws IOException {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(URLEncoder.encode(field.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(field.getValue(), "UTF-8"));
        }
        return send("POST", path, "text/html", "application/x-www-form-urlencoded", body.toString(), null);
    }

    Response postJson(String path, String accept, String json, String csrfToken) throws IOException {
        return send("POST", path, accept, "application/json", json, csrfToken);
    }

    private Response send(String method, String path, String accept, String contentType, String body, String csrfToken)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(base + path).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", accept);
        if (!cookies.isEmpty()) {
            StringBuilder cookie = new StringBuilder();
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                cookie.append(cookie.length() == 0 ? "" : "; ").append(entry.getKey()).append('=').append(entry.getValue());
            }
            connection.setRequestProperty("Cookie", cookie.toString());
        }
        if (csrfToken != null) {
            connection.setRequestProperty("X-CSRF-TOKEN", csrfToken);
        }
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", contentType + "; charset=UTF-8");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        try {
            int status = connection.getResponseCode();
            List<String> setCookies = connection.getHeaderFields().get("Set-Cookie");
            if (setCookies != null) {
                for (String setCookie : setCookies) {
                    String pair = setCookie.split(";", 2)[0];
                    int eq = pair.indexOf('=');
                    cookies.put(pair.substring(0, eq), pair.substring(eq + 1));
                }
            }
            InputStream in = status < 400 ? connection.getInputStream() : connection.getErrorStream();
            return new Response(status, read(in), connection.getHeaderFields());
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (InputStream input = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = input.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
