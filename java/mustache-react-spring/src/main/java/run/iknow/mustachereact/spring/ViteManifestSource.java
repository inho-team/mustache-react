package run.iknow.mustachereact.spring;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

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
            manifest.compareAndSet(null, ViteManifest.read(resource.getInputStream()));
            return manifest.get();
        } catch (IOException ex) {
            throw new IllegalStateException("mustache-react: could not read the Vite manifest at '" + location + "'", ex);
        }
    }
}
