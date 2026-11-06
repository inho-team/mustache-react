package run.iknow.mustachereact.spring;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import run.iknow.mustachereact.core.Json;
import run.iknow.mustachereact.core.MustacheReactVersion;
import run.iknow.mustachereact.core.ViteManifest;

/**
 * Loads the Vite manifest on first use and keeps it.
 *
 * <p>A missing manifest is not cached, so building the frontend while the application runs is picked up by the
 * next request.
 */
public class ViteManifestSource {

    private final ResourceLoader resourceLoader;
    private final String location;
    private final AtomicReference<ViteManifest> manifest = new AtomicReference<>();

    /**
     * @param resourceLoader loads the manifest
     * @param location location of the manifest, e.g. {@code classpath:/static/mustache-react/.vite/manifest.json}
     */
    public ViteManifestSource(ResourceLoader resourceLoader, String location) {
        this.resourceLoader = resourceLoader;
        this.location = location;
    }

    /**
     * @return the manifest
     * @throws IllegalStateException if the manifest does not exist or cannot be read
     */
    public ViteManifest get() {
        ViteManifest current = manifest.get();
        if (current != null) {
            return current;
        }
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("mustache-react: Vite manifest not found at '" + location
                    + "'. Build the frontend first (for example 'pnpm build' in src/main/frontend), "
                    + "or set 'mustache-react.manifest-location'.");
        }
        try {
            if (manifest.compareAndSet(null, ViteManifest.read(resource.getInputStream()))) {
                checkBuildVersion(resource);
            }
            return manifest.get();
        } catch (IOException ex) {
            throw new IllegalStateException("mustache-react: could not read the Vite manifest at '" + location + "'", ex);
        }
    }

    /**
     * Warns when the frontend was built by another version of {@code @mustache-react/vite-plugin}: the Java side
     * and the build share conventions (entry name, file layout) and are released together.
     */
    private void checkBuildVersion(Resource manifestResource) {
        String expected = MustacheReactVersion.get();
        try {
            Resource info = manifestResource.createRelative("../" + BUILD_INFO);
            if (!info.exists()) {
                return; // built before the build info existed, or by a custom build
            }
            Object parsed;
            try (InputStream in = info.getInputStream()) {
                parsed = Json.parse(StreamUtils.copyToString(in, StandardCharsets.UTF_8));
            }
            Object built = parsed instanceof Map ? ((Map<?, ?>) parsed).get("version") : null;
            if (built != null && !"unknown".equals(expected) && !expected.equals(built)) {
                LOGGER.warn("mustache-react: the frontend was built with @mustache-react/vite-plugin " + built
                        + " but the application runs mustache-react " + expected
                        + ". Use the same version for the Java artifacts and the npm packages.");
                mismatch = String.valueOf(built);
            }
        } catch (IOException | IllegalArgumentException ex) {
            LOGGER.debug("mustache-react: could not read " + BUILD_INFO, ex);
        }
    }

    /** @return the vite plugin version that built the frontend when it differs from this library, else null */
    String getVersionMismatch() {
        return mismatch;
    }

    private volatile String mismatch;

    /** Written by the vite plugin next to {@code .vite/}. */
    static final String BUILD_INFO = "mustache-react-build.json";

    private static final Log LOGGER = LogFactory.getLog(ViteManifestSource.class);
}
