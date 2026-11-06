package run.iknow.mustachereact.spring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import run.iknow.mustachereact.core.Html;
import run.iknow.mustachereact.core.Protocol;

/**
 * Builds the server context of a page from the {@link ServerContextContributor}s and writes it as the
 * {@code <script type="application/json" id="mr-context">} element the browser runtime reads.
 */
public class ServerContextFactory {

    private static final Log LOGGER = LogFactory.getLog(ServerContextFactory.class);

    private final List<ServerContextContributor> contributors;
    private final PropsSerializer serializer;

    /**
     * @param contributors the contributors, in order
     * @param serializer writes the context as JSON with the application's message converters
     */
    public ServerContextFactory(List<ServerContextContributor> contributors, PropsSerializer serializer) {
        this.contributors = new ArrayList<>(contributors);
        this.serializer = serializer;
    }

    /**
     * @param extra entries added after the contributors' (for example validation errors), may be {@code null}
     * @return the context of the current request
     */
    public Map<String, Object> create(Map<String, Object> extra) {
        Map<String, Object> context = new LinkedHashMap<>();
        for (ServerContextContributor contributor : contributors) {
            try {
                contributor.contribute(context);
            } catch (RuntimeException ex) {
                LOGGER.warn("mustache-react: " + contributor.getClass().getName() + " failed; its entries are missing "
                        + "from the server context", ex);
            }
        }
        if (extra != null) {
            context.putAll(extra);
        }
        return context;
    }

    /**
     * @param extra entries added after the contributors', may be {@code null}
     * @return the context of the current request as JSON
     */
    public String json(Map<String, Object> extra) {
        return serializer.serialize(create(extra));
    }

    /**
     * @param extra entries added after the contributors', may be {@code null}
     * @return the {@code <script>} element holding the context
     */
    public String scriptTag(Map<String, Object> extra) {
        return scriptTagFor(json(extra));
    }

    /**
     * @param json the context as JSON, from {@link #json(Map)}
     * @return the {@code <script>} element holding the context
     */
    public static String scriptTagFor(String json) {
        return "<script type=\"application/json\" id=\"" + Protocol.CONTEXT_SCRIPT_ID + "\">"
                + Html.scriptSafeJson(json) + "</script>\n";
    }
}
