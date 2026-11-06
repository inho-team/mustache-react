package run.iknow.mustachereact.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import run.iknow.mustachereact.core.IslandMarkup;

/**
 * Renders a full HTML page for a React page component, using a Mustache layout.
 *
 * <p>The layout receives:
 * <ul>
 *   <li>{@code {{title}}}, {@code {{lang}}}: page title and language tag</li>
 *   <li>{@code {{{mr.head}}}}: stylesheet and module preload tags, for {@code <head>}</li>
 *   <li>{@code {{{mr.page}}}}: the element React renders into, with its props</li>
 *   <li>{@code {{{mr.scripts}}}}: the server context and the entry script, before {@code </body>}</li>
 * </ul>
 */
public class ReactPageRenderer {

    /** Layout used when the application does not provide one. */
    public static final String DEFAULT_LAYOUT = "classpath:/META-INF/mustache-react/layout.mustache";

    /** Prefix added to {@code @ReactPage} names: {@code orders/OrderList} is the file {@code pages/orders/OrderList.tsx}. */
    public static final String PAGES_DIRECTORY = "pages/";

    private final ResourceLoader resourceLoader;
    private final Mustache.Compiler compiler;
    private final FrontendAssets assets;
    private final ServerContextFactory serverContext;
    private final String layoutLocation;
    private final boolean contentNegotiation;

    private final AtomicReference<Template> layout = new AtomicReference<>();

    /**
     * @param resourceLoader loads the manifest and the layout
     * @param compiler the application's Mustache compiler, so that layouts can use its partials and settings
     * @param assets produces the asset tags (built files or Vite dev server)
     * @param serverContext writes the server context script
     * @param layoutLocation location of the application's layout; the built-in layout is used if it does not exist
     * @param contentNegotiation whether requests for JSON get the data instead of the page
     */
    public ReactPageRenderer(ResourceLoader resourceLoader, Mustache.Compiler compiler, FrontendAssets assets,
            ServerContextFactory serverContext, String layoutLocation, boolean contentNegotiation) {
        this.resourceLoader = resourceLoader;
        this.compiler = compiler;
        this.assets = assets;
        this.serverContext = serverContext;
        this.layoutLocation = layoutLocation;
        this.contentNegotiation = contentNegotiation;
    }

    /**
     * @return whether requests for JSON get the handler's data instead of the page
     */
    public boolean isContentNegotiation() {
        return contentNegotiation;
    }

    /**
     * Renders a page.
     *
     * @param page page name from {@link ReactPage#value()}, e.g. {@code orders/OrderList}
     * @param title page title; empty or {@code null} to use the page name
     * @param propsJson props as a JSON object
     * @param locale locale of the request, for the {@code lang} attribute
     * @return the HTML document
     */
    public String render(String page, String title, String propsJson, Locale locale) {
        return render(page, title, propsJson, null, locale);
    }

    /**
     * Renders a page.
     *
     * @param page page name from {@link ReactPage#value()}, e.g. {@code orders/OrderList}
     * @param title page title; empty or {@code null} to use the page name
     * @param propsJson props as a JSON object
     * @param extraContext entries added to the server context (e.g. validation errors), may be {@code null}
     * @param locale locale of the request, for the {@code lang} attribute
     * @return the HTML document
     */
    public String render(String page, String title, String propsJson, Map<String, Object> extraContext, Locale locale) {
        String component = PAGES_DIRECTORY + IslandMarkup.requireValidName(page);
        Map<String, Object> mr = new HashMap<>();
        mr.put("head", assets.pageHead(component));
        mr.put("page", IslandMarkup.render(component, "mr-page", propsJson, null));
        mr.put("scripts", serverContext.scriptTag(extraContext) + assets.pageScripts(component));

        Map<String, Object> context = new HashMap<>();
        context.put("title", title == null || title.isEmpty() ? page : title);
        context.put("lang", locale == null ? "en" : locale.toLanguageTag());
        context.put("page", page);
        context.put("mr", mr);
        return layout().execute(context);
    }

    private Template layout() {
        Template current = layout.get();
        if (current != null) {
            return current;
        }
        Resource resource = resourceLoader.getResource(layoutLocation);
        if (!resource.exists()) {
            resource = resourceLoader.getResource(DEFAULT_LAYOUT);
        }
        try (InputStream in = resource.getInputStream();
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            layout.compareAndSet(null, compiler.compile(reader));
            return layout.get();
        } catch (IOException ex) {
            throw new IllegalStateException("mustache-react: could not read the layout at '" + resource.getDescription() + "'", ex);
        }
    }
}
