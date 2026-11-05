package run.iknow.mustachereact.spring;

import java.util.List;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

/**
 * Decides whether a {@code @ReactPage} request wants the HTML page or the JSON data.
 */
final class AcceptHeaders {

    private static final MediaType XHTML = MediaType.APPLICATION_XHTML_XML;

    private AcceptHeaders() {
    }

    /**
     * Returns {@code true} when the request names {@code text/html} (or XHTML) explicitly and does not rank JSON
     * higher. Browsers navigating to a page send {@code text/html}; {@code fetch}, curl and API clients usually send
     * {@code *}{@code /*} or {@code application/json}, and get JSON.
     */
    static boolean prefersHtml(String[] acceptHeaders) {
        if (acceptHeaders == null || acceptHeaders.length == 0) {
            return false;
        }
        double html = 0;
        double json = 0;
        for (String header : acceptHeaders) {
            List<MediaType> types;
            try {
                types = MediaType.parseMediaTypes(header);
            } catch (InvalidMediaTypeException ex) {
                continue;
            }
            for (MediaType type : types) {
                double quality = type.getQualityValue();
                if (isHtml(type)) {
                    html = Math.max(html, quality);
                } else if (isJson(type)) {
                    json = Math.max(json, quality);
                }
            }
        }
        return html > 0 && html >= json;
    }

    private static boolean isHtml(MediaType type) {
        return (MediaType.TEXT_HTML.getType().equals(type.getType()) && MediaType.TEXT_HTML.getSubtype().equals(type.getSubtype()))
                || (XHTML.getType().equals(type.getType()) && XHTML.getSubtype().equals(type.getSubtype()));
    }

    private static boolean isJson(MediaType type) {
        return "application".equals(type.getType())
                && ("json".equals(type.getSubtype()) || type.getSubtype().endsWith("+json"));
    }
}
