package run.iknow.mustachereact.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

class PropsSerializerTest {

    /** Stands in for the Jackson converter: writes {"value": "<toString>"} for any object. */
    static class FakeJsonConverter implements HttpMessageConverter<Object> {
        @Override
        public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return false;
        }

        @Override
        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return MediaType.APPLICATION_JSON.includes(mediaType);
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return Collections.singletonList(MediaType.APPLICATION_JSON);
        }

        @Override
        public Object read(Class<?> clazz, HttpInputMessage inputMessage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws IOException {
            String json = o instanceof List ? "[1]" : "{\"value\":\"" + o + "\"}";
            outputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            outputMessage.getBody().write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private final PropsSerializer serializer = new PropsSerializer(
            Arrays.<HttpMessageConverter<?>>asList(new StringHttpMessageConverter(), new FakeJsonConverter()));

    @Test
    void usesTheFirstJsonConverterButNeverTheStringConverter() {
        assertThat(serializer.serialize("주문")).isEqualTo("{\"value\":\"주문\"}");
    }

    @Test
    void nullMeansNoProps() {
        assertThat(serializer.serialize(null)).isEqualTo("{}");
    }

    @Test
    void rejectsValuesThatAreNotJsonObjects() {
        assertThatThrownBy(() -> serializer.serialize(Collections.singletonList(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Props must be a JSON object");
    }

    @Test
    void explainsAMissingJsonConverter() {
        PropsSerializer withoutJson = new PropsSerializer(
                Collections.<HttpMessageConverter<?>>singletonList(new StringHttpMessageConverter()));

        assertThatThrownBy(() -> withoutJson.serialize(new Object()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring-boot-starter-json");
    }
}
