package run.iknow.mustachereact.autoconfigure;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.samskivert.mustache.Mustache;

import run.iknow.mustachereact.core.SsrEngine;
import run.iknow.mustachereact.core.ViteDevServer;
import run.iknow.mustachereact.spring.DevServerFrontendAssets;
import run.iknow.mustachereact.spring.FrontendAssets;
import run.iknow.mustachereact.spring.HttpSsrEngine;
import run.iknow.mustachereact.spring.NodeSsrServer;
import run.iknow.mustachereact.spring.ServerSideRendering;
import run.iknow.mustachereact.spring.LocaleContextContributor;
import run.iknow.mustachereact.spring.MessageKeys;
import run.iknow.mustachereact.spring.MessageLambda;
import run.iknow.mustachereact.spring.MessagesContextContributor;
import run.iknow.mustachereact.spring.SecurityContextContributors;
import run.iknow.mustachereact.spring.ServerContextContributor;
import run.iknow.mustachereact.spring.ServerContextFactory;
import run.iknow.mustachereact.spring.ManifestFrontendAssets;
import run.iknow.mustachereact.spring.MustacheReactHelpers;
import run.iknow.mustachereact.spring.MustacheReactModelAdvice;
import run.iknow.mustachereact.spring.PropsSerializer;
import run.iknow.mustachereact.spring.ReactIslandLambda;
import run.iknow.mustachereact.spring.ReactPageHandlerAdapterPostProcessor;
import run.iknow.mustachereact.spring.ReactPageRenderer;
import run.iknow.mustachereact.spring.VersionContextContributor;
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
            ObjectProvider<Mustache.Compiler> compiler, FrontendAssets assets, ServerContextFactory serverContext,
            ServerSideRendering ssr) {
        return new ReactPageRenderer(resourceLoader, compiler.getIfAvailable(Mustache::compiler), assets,
                serverContext, ssr, properties.getLayout(), properties.isContentNegotiation());
    }

    /**
     * Server-side rendering policy ({@code mustache-react.ssr}). With {@code off}, the default, nothing is started.
     */
    @Bean
    @ConditionalOnMissingBean
    public ServerSideRendering mustacheReactServerSideRendering(MustacheReactProperties properties,
            ObjectProvider<SsrEngine> engine) {
        MustacheReactProperties.SsrMode mode = properties.getSsr();
        if (mode == MustacheReactProperties.SsrMode.OFF) {
            return ServerSideRendering.off();
        }
        SsrEngine ssrEngine = engine.getIfAvailable();
        if (ssrEngine == null && mode == MustacheReactProperties.SsrMode.ALWAYS) {
            throw new IllegalStateException("mustache-react.ssr=always, but there is no SsrEngine");
        }
        return new ServerSideRendering(ServerSideRendering.Mode.valueOf(mode.name()), ssrEngine);
    }

    /** {@code mustache-react.ssr=auto|always}: an engine calling the SSR server over HTTP. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnExpression("!'${mustache-react.ssr:off}'.equalsIgnoreCase('off')")
    static class SsrConfiguration {

        /** Production: runs the SSR bundle from the jar as a Node.js sidecar. */
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "dev-server.enabled", havingValue = "false",
                matchIfMissing = true)
        NodeSsrServer mustacheReactSsrServer(ResourceLoader resourceLoader, MustacheReactProperties properties) {
            MustacheReactProperties.DevServer dev = properties.getDevServer();
            MustacheReactProperties.SsrServer ssr = properties.getSsrServer();
            return new NodeSsrServer(resourceLoader, ssr.getBundleLocation(), dev.getNode(),
                    new File(dev.getNodeInstallDir()).getAbsoluteFile(), ssr.getStartupTimeout().toMillis(),
                    properties.getSsr() == MustacheReactProperties.SsrMode.ALWAYS);
        }

        @Bean
        @ConditionalOnMissingBean(SsrEngine.class)
        SsrEngine mustacheReactSsrEngine(MustacheReactProperties properties, ObjectProvider<NodeSsrServer> server) {
            int timeout = (int) properties.getSsrServer().getTimeout().toMillis();
            if (properties.getDevServer().isEnabled()) {
                // Development: the Vite dev server renders with the current sources.
                String endpoint = properties.getDevServer().getUrl().replaceAll("/+$", "") + HttpSsrEngine.RENDER_PATH;
                return new HttpSsrEngine(() -> endpoint, timeout);
            }
            return new HttpSsrEngine(() -> {
                NodeSsrServer node = server.getIfAvailable();
                return node == null ? null : node.getEndpoint();
            }, timeout);
        }
    }

    /**
     * The server context of every page: the {@link ServerContextContributor} beans, in {@code @Order}.
     */
    @Bean
    @ConditionalOnMissingBean
    public ServerContextFactory mustacheReactServerContext(ObjectProvider<ServerContextContributor> contributors,
            PropsSerializer propsSerializer) {
        return new ServerContextFactory(contributors.orderedStream().collect(Collectors.toList()), propsSerializer);
    }

    @Bean
    public ServerContextContributor mustacheReactVersionContext() {
        return new VersionContextContributor();
    }

    @Bean
    public ServerContextContributor mustacheReactLocaleContext(MustacheReactProperties properties) {
        return new LocaleContextContributor(properties.getI18n().getParam(), properties.getI18n().getLocales());
    }

    /** The keys of the message bundles, to expand patterns such as {@code orders.*}. */
    @Bean
    @ConditionalOnMissingBean
    public MessageKeys mustacheReactMessageKeys(Environment environment, ResourceLoader resourceLoader) {
        // The same bundles as Spring Boot's MessageSourceAutoConfiguration.
        String basenames = environment.getProperty("spring.messages.basename", "messages");
        String encoding = environment.getProperty("spring.messages.encoding", "UTF-8");
        return new MessageKeys(ResourcePatternUtils.getResourcePatternResolver(resourceLoader),
                StringUtils.commaDelimitedListToSet(StringUtils.trimAllWhitespace(basenames)), Charset.forName(encoding));
    }

    @Bean
    public ServerContextContributor mustacheReactMessagesContext(MessageSource messageSource,
            MustacheReactProperties properties, MessageKeys messageKeys) {
        return new MessagesContextContributor(messageSource, properties.getContext().getMessages(), messageKeys::all);
    }

    /** CSRF token and signed-in user, with Spring Security. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {
            "org.springframework.security.core.context.SecurityContextHolder",
            "org.springframework.security.web.csrf.CsrfToken",
    })
    static class SecurityContextConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "context.csrf", matchIfMissing = true)
        ServerContextContributor mustacheReactCsrfContext() {
            return SecurityContextContributors.csrf();
        }

        @Bean
        @ConditionalOnProperty(prefix = MustacheReactProperties.PREFIX, name = "context.user", matchIfMissing = true)
        ServerContextContributor mustacheReactUserContext() {
            return SecurityContextContributors.user();
        }
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
    public ReactIslandLambda reactIslandLambda(PropsSerializer propsSerializer, ServerSideRendering ssr,
            ServerContextFactory serverContext) {
        return new ReactIslandLambda(propsSerializer, ssr, serverContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public MustacheReactHelpers mustacheReactHelpers(FrontendAssets assets, ServerContextFactory serverContext,
            MessageSource messageSource) {
        return new MustacheReactHelpers(assets, serverContext, new MessageLambda(messageSource));
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

    /** Mustache views in UTF-8 whatever the request language, see {@link MustacheCharsetPostProcessor}. */
    @Bean
    static BeanPostProcessor mustacheReactMustacheCharset(Environment environment) {
        return new MustacheCharsetPostProcessor(environment);
    }

    /**
     * Installs the {@code @ReactPage} return value handler. Static, as bean post-processors must be created before
     * regular beans.
     */
    @Bean
    public static ReactPageHandlerAdapterPostProcessor reactPageHandlerAdapterPostProcessor(
            ObjectProvider<ReactPageRenderer> renderer, ObjectProvider<MessageSource> messageSource) {
        return new ReactPageHandlerAdapterPostProcessor(renderer, messageSource);
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
