package run.iknow.mustachereact.gradle;

import java.io.IOException;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import run.iknow.mustachereact.build.BuildLog;
import run.iknow.mustachereact.build.NodeInstaller;
import run.iknow.mustachereact.build.NodeRuntime;

/**
 * Node.js settings shared by the tasks.
 */
public interface NodeSettings {

    /** @return whether to download Node.js */
    @Input
    Property<Boolean> getNodeDownload();

    /** @return the version to download */
    @Input
    Property<String> getNodeVersion();

    /** @return the distribution URL */
    @Input
    Property<String> getNodeDistributionUrl();

    /** @return the install directory; not an input, it is a cache */
    @Internal
    DirectoryProperty getNodeInstallDir();

    /**
     * @param logger the task logger
     * @return the Node.js installation, downloaded first if needed
     * @throws IOException if the download fails
     */
    default NodeRuntime node(Logger logger) throws IOException {
        if (!getNodeDownload().get()) {
            return NodeRuntime.fromPath();
        }
        return new NodeInstaller(getNodeInstallDir().get().getAsFile().toPath(), getNodeDistributionUrl().get(),
                log(logger)).install(getNodeVersion().get());
    }

    /**
     * @param logger the task logger
     * @return the logger as a {@link BuildLog}
     */
    static BuildLog log(Logger logger) {
        return new BuildLog() {
            @Override
            public void info(String message) {
                logger.lifecycle(message);
            }

            @Override
            public void warn(String message) {
                logger.warn(message);
            }
        };
    }

    /** @return the default version */
    static String defaultVersion() {
        return NodeInstaller.DEFAULT_VERSION;
    }
}
