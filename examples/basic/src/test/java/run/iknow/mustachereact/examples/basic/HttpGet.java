package run.iknow.mustachereact.examples.basic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Tiny HTTP client that works on Java 8 and with every supported Spring Boot version. */
final class HttpGet {

    final int status;
    final String contentType;
    final String vary;
    final String body;

    private HttpGet(int status, String contentType, String vary, String body) {
        this.status = status;
        this.contentType = contentType == null ? "" : contentType;
        this.vary = vary == null ? "" : vary;
        this.body = body;
    }

    static HttpGet of(String url, String accept) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        // HttpURLConnection sends a default Accept header unless one is set; an empty value stands for "none".
        connection.setRequestProperty("Accept", accept == null ? "" : accept);
        try {
            int status = connection.getResponseCode();
            InputStream in = status < 400 ? connection.getInputStream() : connection.getErrorStream();
            return new HttpGet(status, connection.getContentType(), connection.getHeaderField("Vary"), read(in));
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
