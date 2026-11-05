package run.iknow.mustachereact.spring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

/**
 * Turns a handler's return value into the JSON props of a React page.
 *
 * <p>Uses the same {@link HttpMessageConverter}s as Spring MVC, so the props are exactly the JSON the endpoint
 * returns to API clients: Jackson 2 on Spring Boot 2.7/3.x, Jackson 3 on Spring Boot 4, Gson, or any converter and
 * customization the application configured ({@code @JsonIgnore}, naming strategy, date format, ...).
 */
public class PropsSerializer {

    private final Supplier<List<HttpMessageConverter<?>>> converterSource;
    private volatile List<HttpMessageConverter<?>> converters;

    /**
     * @param converters the message converters, usually those of the {@code RequestMappingHandlerAdapter}
     */
    public PropsSerializer(List<HttpMessageConverter<?>> converters) {
        this.converters = Collections.unmodifiableList(new ArrayList<>(converters));
        this.converterSource = null;
    }

    /**
     * Creates a serializer that obtains the converters on first use, for example from the
     * {@code RequestMappingHandlerAdapter} once the application context is ready.
     *
     * @param converters supplies the message converters
     */
    public PropsSerializer(Supplier<List<HttpMessageConverter<?>>> converters) {
        this.converterSource = converters;
    }

    private List<HttpMessageConverter<?>> converters() {
        List<HttpMessageConverter<?>> current = converters;
        if (current == null) {
            current = Collections.unmodifiableList(new ArrayList<>(converterSource.get()));
            converters = current;
        }
        return current;
    }

    /**
     * Serializes props.
     *
     * @param value the props object, typically a DTO or a {@link java.util.Map}; {@code null} means no props
     * @return a JSON object
     * @throws IllegalArgumentException if the value does not serialize to a JSON object
     * @throws IllegalStateException if no converter can write the value as JSON
     */
    @SuppressWarnings("unchecked")
    public String serialize(Object value) {
        if (value == null) {
            return "{}";
        }
        for (HttpMessageConverter<?> converter : converters()) {
            if (converter instanceof StringHttpMessageConverter
                    || !converter.canWrite(value.getClass(), MediaType.APPLICATION_JSON)) {
                continue;
            }
            BufferedMessage message = new BufferedMessage();
            try {
                ((HttpMessageConverter<Object>) converter).write(value, MediaType.APPLICATION_JSON, message);
            } catch (IOException ex) {
                throw new IllegalStateException("Could not serialize props of type " + value.getClass().getName(), ex);
            }
            String json = message.text().trim();
            if (!json.startsWith("{")) {
                throw new IllegalArgumentException("Props must be a JSON object, but " + value.getClass().getName()
                        + " was written as " + abbreviate(json)
                        + ". Return a DTO or a Map from the @ReactPage method, not a list or a simple value.");
            }
            return json;
        }
        throw new IllegalStateException("No HttpMessageConverter can write " + value.getClass().getName()
                + " as application/json. Is Jackson on the classpath (spring-boot-starter-json)?");
    }

    private static String abbreviate(String json) {
        return json.length() <= 40 ? json : json.substring(0, 40) + "...";
    }

    private static final class BufferedMessage implements HttpOutputMessage {

        private final HttpHeaders headers = new HttpHeaders();
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();

        @Override
        public OutputStream getBody() {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        String text() {
            MediaType contentType = headers.getContentType();
            Charset charset = contentType != null && contentType.getCharset() != null
                    ? contentType.getCharset()
                    : StandardCharsets.UTF_8;
            return new String(body.toByteArray(), charset);
        }
    }
}
