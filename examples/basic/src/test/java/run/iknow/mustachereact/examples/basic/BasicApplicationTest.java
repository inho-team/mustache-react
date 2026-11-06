package run.iknow.mustachereact.examples.basic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import run.iknow.mustachereact.autoconfigure.MustacheReactProperties;

/**
 * Starts the real application on a random port. Uses only APIs that exist in Spring Boot 2.7, 3.x and 4.x.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "mustache-react.ssr=off")
class BasicApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Test
    void autoConfigurationIsPickedUp() {
        assertThat(context.getBeansOfType(MustacheReactProperties.class)).hasSize(1);
    }

    @Test
    void rendersHomePage() throws IOException {
        String port = environment.getProperty("local.server.port");
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/").openConnection();
        try {
            assertThat(connection.getResponseCode()).isEqualTo(200);
            assertThat(read(connection.getInputStream()))
                    .contains("mustache-react example")
                    .contains("Spring Boot " + SpringBootVersion.getVersion());
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
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
