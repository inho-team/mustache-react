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
import run.iknow.mustachereact.core.PageAssets;
import run.iknow.mustachereact.core.ViteManifest;

/**
 * Renders a full HTML page for a React page component, using a Mustache layout.
 *
 * <p>The layout receives:
 * <ul>
 *   <li>{@code {{title}}}, {@code {{lang}}}: page title and language tag</li>
 *   <li>{@code {{{mr.head}}}}: stylesheet and module preload tags, for {@code <head>}</li>
 *   <li>{@code {{{mr.page}}}}: the element React renders into, with its props</li>
 *   <li>{@code {{{mr.scripts}}}}: the entry script tag, before {@code </body>}</li>
 * </ul>
 */
public class ReactPageRenderer {

    /** Layout used when the application does not provide one. */
    public static final String DEFAULT_LAYOUT = "classpath:/META-INF/mustache-react/layout.mustache";

    /** Prefix added to {@code @ReactPage} names: {@code orders/OrderList} is the file {@code pages/orders/OrderList.tsx}. */
    public static final String PAGES_DIRECTORY = "pages/";

    private final ResourceLoader resourceLoader;
    private final Mustache.Compiler compiler;
    private final String manifestLocation;
    private final String layoutLocation;
    private final String publicPath;
    private final boolean contentNegotiation;

    private final AtomicReference<ViteManifest> manifest = new AtomicReference<>();
    private final AtomicReference<Template> layout = new AtomicReference<>();

    /**
     * @param resourceLoader loads the manifest and the layout
     * @param compiler the application's Mustache compiler, so that layouts can use its partials and settings
     * @param manifestLocation location of the Vite manifest, e.g. {@code classpath:/static/mustache-react/.vite/manifest.json}
     * @param layoutLocation location of the application's layout; the built-in layout is used if it does not exist
     * @param publicPath URL path the built assets are served from, e.g. {@code /mustache-react/}
     * @param contentNegotiation whether requests for JSON get the data instead of the page
     */
    public ReactPageRenderer(ResourceLoader resourceLoader, Mustache.Compiler compiler, String manifestLocation,
            String layoutLocation, String publicPath, boolean contentNegotiation) {
        this.resourceLoader = resourceLoader;
        this.compiler = compiler;
        this.manifestLocation = manifestLocation;
        this.layoutLocation = layoutLocation;
        this.publicPath = publicPath;
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
        String component = PAGES_DIRECTORY + IslandMarkup.requireValidName(page);
        PageAssets assets = manifest().assetsFor(component);

        Map<String, Object> mr = new HashMap<>();
        mr.put("head", assets.headTags(publicPath));
        mr.put("page", IslandMarkup.render(component, "mr-page", propsJson, null));
        mr.put("scripts", assets.scriptTags(publicPath));

        Map<String, Object> context = new HashMap<>();
        context.put("title", title == null || title.isEmpty() ? page : title);
        context.put("lang", locale == null ? "en" : locale.toLanguageTag());
        context.put("page", page);
        context.put("mr", mr);
        return layout().execute(context);
    }

    private ViteManifest manifest() {
        ViteManifest current = manifest.get();
        if (current != null) {
            return current;
        }
        Resource resource = resourceLoader.getResource(manifestLocation);
        if (!resource.exists()) {
            // Not cached: once the frontend is built, the next request picks it up.
            throw new IllegalStateException("mustache-react: Vite manifest not found at '" + manifestLocation
                    + "'. Build the frontend first (for example 'pnpm build' in src/main/frontend), "
                    + "or set 'mustache-react.manifest-location'.");
        }
        try {
            ViteManifest loaded = ViteManifest.read(resource.getInputStream());
            manifest.compareAndSet(null, loaded);
            return manifest.get();
        } catch (IOException ex) {
            throw new IllegalStateException("mustache-react: could not read the Vite manifest at '" + manifestLocation + "'", ex);
        }
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
