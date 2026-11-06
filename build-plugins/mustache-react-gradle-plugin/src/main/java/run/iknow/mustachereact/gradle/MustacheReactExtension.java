package run.iknow.mustachereact.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * The {@code mustacheReact { }} block. Every setting has a default; most projects configure nothing.
 *
 * <pre>
 * mustacheReact {
 *     nodeVersion = "24.22.2"      // Node.js downloaded into .mustache-react/node
 *     nodeDownload = false         // use the node on the PATH instead
 *     packageManager = "pnpm"      // default: detected from package.json / lock file
 * }
 * </pre>
 */
public abstract class MustacheReactExtension {

    private Object processorDependency;

    /** @return the frontend directory holding {@code package.json}. Default: {@code src/main/frontend} */
    public abstract DirectoryProperty getFrontendDir();

    /** @return the URL path Spring serves the assets from; must match {@code mustache-react.public-path}. Default: {@code /mustache-react/} */
    public abstract Property<String> getPublicPath();

    /** @return whether to build the frontend into the resources. Default: {@code true}, or {@code false} with {@code -PmustacheReact.skipFrontend} */
    public abstract Property<Boolean> getFrontendBuild();

    /** @return whether to install the frontend dependencies before building. Default: {@code true} */
    public abstract Property<Boolean> getInstall();

    /** @return {@code auto}, {@code npm}, {@code pnpm} or {@code yarn}. Default: {@code auto} */
    public abstract Property<String> getPackageManager();

    /** @return whether to download Node.js instead of using the one on the {@code PATH}. Default: {@code true} */
    public abstract Property<Boolean> getNodeDownload();

    /** @return the Node.js version to download */
    public abstract Property<String> getNodeVersion();

    /** @return where to download Node.js from. Default: {@code https://nodejs.org/dist} */
    public abstract Property<String> getNodeDistributionUrl();

    /** @return where downloaded Node.js versions are kept. Default: {@code .mustache-react/node} in the project */
    public abstract DirectoryProperty getNodeInstallDir();

    /** @return whether to generate TypeScript types from the Java classes. Default: {@code true} */
    public abstract Property<Boolean> getTypes();

    /** @return the generated TypeScript file. Default: {@code <frontendDir>/generated/types.ts} */
    public abstract RegularFileProperty getTypesFile();

    /**
     * @return the dependency notation of the type generator, or {@code null} for
     *         {@code run.iknow:mustache-react-processor} in the plugin's version
     */
    public Object getProcessorDependency() {
        return processorDependency;
    }

    /**
     * Uses another type generator artifact, e.g. {@code project(":mustache-react-processor")} inside the
     * mustache-react repository.
     *
     * @param processorDependency a dependency notation
     */
    public void setProcessorDependency(Object processorDependency) {
        this.processorDependency = processorDependency;
    }
}
