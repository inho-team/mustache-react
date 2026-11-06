package run.iknow.mustachereact.autoconfigure;

import java.io.File;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.samskivert.mustache.Mustache;

import run.iknow.mustachereact.core.ViteDevServer;
import run.iknow.mustachereact.spring.DevServerFrontendAssets;
import run.iknow.mustachereact.spring.FrontendAssets;
import run.iknow.mustachereact.spring.ManifestFrontendAssets;
import run.iknow.mustachereact.spring.MustacheReactHelpers;
import run.iknow.mustachereact.spring.MustacheReactModelAdvice;
import run.iknow.mustachereact.spring.PropsSerializer;
import run.iknow.mustachereact.spring.ReactIslandLambda;
import run.iknow.mustachereact.spring.ReactPageHandlerAdapterPostProcessor;
import run.iknow.mustachereact.spring.ReactPageRenderer;
import run.iknow.mustachereact.spring.ViteDevServerProcess;
import run.iknow.mustachereact.spring.ViteManifestSource;

/**
 * Auto-configuration for mustache-react.
 *
 * <p>Registered through {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * which Spring Boot reads from 2.7 onward. Ordering against other auto-configurations uses class names as strings,
 * because Spring Boot 4 moved the Mustache auto-configuration to a different package.
 */
@AutoConfiguration(afterName = {
        // Spring Boot 2.7, 3.x
        "org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration",
        // Spring Boot 4.x
        "org.springframework.boot.mustache.autoconfigure.MustacheAutoConfiguration",
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = {
        "com.samskivert.mustache.Mustache",
        "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter",
})
@ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MustacheReactProperties.class)
public class MustacheReactAutoConfiguration {

    private static final Log LOGGER = LogFactory.getLog(MustacheReactAutoConfiguration.class);

    /**
     * Renders {@code @ReactPage} pages. Uses the application's Mustache compiler when there is one, so a custom
     * layout can include the application's partials.
     */
    @Bean
    @ConditionalOnMissingBean
    public ViteManifestSource mustacheReactManifest(ResourceLoader resourceLoader, MustacheReactProperties properties) {
        return new ViteManifestSource(resourceLoader, properties.getManifestLocation());
    }

    /**
     * Asset tags: from the Vite dev server when {@code mustache-react.dev-server.enabled=true}, otherwise from the
     * built files listed in the manifest.
     */
    @Bean
    @ConditionalOnMissingBean
    public FrontendAssets mustacheReactFrontendAssets(MustacheReactProperties properties,
            ObjectProvider<ViteManifestSource> manifest) {
        MustacheReactProperties.DevServer dev = properties.getDevServer();
        if (dev.isEnabled()) {
            String url = dev.getUrl().replaceAll("/+$", "") + "/" + properties.getPublicPath().replaceAll("^/+", "");
            LOGGER.info("mustache-react: loading the frontend from the Vite dev server at " + url);
            return new DevServerFrontendAssets(new ViteDevServer(url));
        }
        return new ManifestFrontendAssets(manifest.getObject(), properties.getPublicPath());
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactPageRenderer reactPageRenderer(ResourceLoader resourceLoader, MustacheReactProperties properties,
            ObjectProvider<Mustache.Compiler> compiler, FrontendAssets assets) {
        return new ReactPageRenderer(resourceLoader, compiler.getIfAvailable(Mustache::compiler), assets,
                properties.getLayout(), properties.isContentNegotiation());
    }

    /**
     * Serializes island props with Spring MVC's message converters, looked up on first use.
     */
    @Bean
    @ConditionalOnMissingBean
    public PropsSerializer mustacheReactPropsSerializer(ObjectProvider<RequestMappingHandlerAdapter> adapter) {
        return new PropsSerializer(() -> adapter.getObject().getMessageConverters());
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactIslandLambda reactIslandLambda(PropsSerializer propsSerializer) {
        return new ReactIslandLambda(propsSerializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public MustacheReactHelpers mustacheReactHelpers(FrontendAssets assets) {
        return new MustacheReactHelpers(assets);
    }

    /**
     * Makes {@code {{#react}}}, {@code {{{mr.head}}}} and {@code {{{mr.scripts}}}} available in every Mustache view.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "template.model-attributes", matchIfMissing = true)
    public MustacheReactModelAdvice mustacheReactModelAdvice(MustacheReactProperties properties,
            ReactIslandLambda react, MustacheReactHelpers helpers) {
        return new MustacheReactModelAdvice(properties.getTemplate().getReactName(), react,
                properties.getTemplate().getHelpersName(), helpers);
    }

    /**
     * Installs the {@code @ReactPage} return value handler. Static, as bean post-processors must be created before
     * regular beans.
     */
    @Bean
    public static ReactPageHandlerAdapterPostProcessor reactPageHandlerAdapterPostProcessor(
            ObjectProvider<ReactPageRenderer> renderer) {
        return new ReactPageHandlerAdapterPostProcessor(renderer);
    }

    /** Development mode: {@code mustache-react.dev-server.enabled=true}. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "dev-server.enabled", havingValue = "true")
    static class DevServerConfiguration {

        /**
         * Starts the Vite dev server with the application, unless {@code mustache-react.dev-server.auto-start=false}.
         */
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "dev-server.auto-start", havingValue = "true",
                matchIfMissing = true)
        ViteDevServerProcess mustacheReactViteDevServer(MustacheReactProperties properties) {
            MustacheReactProperties.DevServer dev = properties.getDevServer();
            return new ViteDevServerProcess(new File(properties.getFrontendDir()).getAbsoluteFile(),
                    URI.create(dev.getUrl()), properties.getPublicPath(), dev.getNode(),
                    new File(dev.getNodeInstallDir()).getAbsoluteFile(), dev.getStartupTimeout().toMillis());
        }
    }
}
