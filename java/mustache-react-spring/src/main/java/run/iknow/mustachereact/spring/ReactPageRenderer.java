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
    private final ServerSideRendering ssr;
    private final String layoutLocation;
    private final boolean contentNegotiation;

    private final AtomicReference<Template> layout = new AtomicReference<>();
    private Map<String, Object> layoutModel = new HashMap<>();
    private String helpersName = "mr";
    private MessageLambda messages;
    private boolean cacheLayout = true;

    /**
     * @param resourceLoader loads the manifest and the layout
     * @param compiler the application's Mustache compiler, so that layouts can use its partials and settings
     * @param assets produces the asset tags (built files or Vite dev server)
     * @param serverContext writes the server context script
     * @param ssr server-side rendering, or {@link ServerSideRendering#off()}
     * @param layoutLocation location of the application's layout; the built-in layout is used if it does not exist
     * @param contentNegotiation whether requests for JSON get the data instead of the page
     */
    public ReactPageRenderer(ResourceLoader resourceLoader, Mustache.Compiler compiler, FrontendAssets assets,
            ServerContextFactory serverContext, ServerSideRendering ssr, String layoutLocation,
            boolean contentNegotiation) {
        this.resourceLoader = resourceLoader;
        this.compiler = compiler;
        this.assets = assets;
        this.serverContext = serverContext;
        this.ssr = ssr;
        this.layoutLocation = layoutLocation;
        this.contentNegotiation = contentNegotiation;
    }

    /**
     * @param model extra layout variables, e.g. the island section ({@code react}) so that layouts can contain
     *     islands
     * @return this renderer
     */
    public ReactPageRenderer layoutModel(Map<String, Object> model) {
        this.layoutModel = new HashMap<>(model);
        return this;
    }

    /**
     * @param name the name of the helpers in layouts ({@code mr}, as in Mustache views); the built-in layout always
     *     finds them as {@code mr} too
     * @return this renderer
     */
    public ReactPageRenderer helpersName(String name) {
        this.helpersName = name;
        return this;
    }

    /**
     * @param messages {@code {{#mr.t}}key{{/mr.t}}} in layouts; {@code null} for none
     * @return this renderer
     */
    public ReactPageRenderer messages(MessageLambda messages) {
        this.messages = messages;
        return this;
    }

    /**
     * @param cache whether the layout is compiled once; {@code false} reads it again for every page, for development
     * @return this renderer
     */
    public ReactPageRenderer cacheLayout(boolean cache) {
        this.cacheLayout = cache;
        return this;
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
        String contextJson = serverContext.json(extraContext);
        String ssrHtml = ssr.render(component, propsJson, () -> contextJson);
        Map<String, Object> mr = new HashMap<>();
        mr.put("head", assets.pageHead(component));
        mr.put("page", IslandMarkup.render(component, "mr-page", propsJson, ssrHtml));
        mr.put("scripts", ServerContextFactory.scriptTagFor(contextJson) + assets.pageScripts(component));
        mr.put("lang", locale == null ? "en" : locale.toLanguageTag());
        if (messages != null) {
            mr.put("t", messages);
        }

        Map<String, Object> context = new HashMap<>(layoutModel);
        context.put("title", title == null || title.isEmpty() ? page : title);
        context.put("lang", locale == null ? "en" : locale.toLanguageTag());
        context.put("page", page);
        context.put("mr", mr);
        context.put(helpersName, mr);
        return layout().execute(context);
    }

    private Template layout() {
        Template current = cacheLayout ? layout.get() : null;
        if (current != null) {
            return current;
        }
        Resource resource = resourceLoader.getResource(layoutLocation);
        if (!resource.exists()) {
            resource = resourceLoader.getResource(DEFAULT_LAYOUT);
        }
        try (InputStream in = resource.getInputStream();
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Template compiled = compiler.compile(reader);
            if (!cacheLayout) {
                return compiled;
            }
            layout.compareAndSet(null, compiled);
            return layout.get();
        } catch (IOException ex) {
            throw new IllegalStateException("mustache-react: could not read the layout at '" + resource.getDescription() + "'", ex);
        }
    }
}
