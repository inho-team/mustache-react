package run.iknow.mustachereact.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import run.iknow.mustachereact.build.FrontendBuild;

/**
 * Installs the frontend dependencies ({@code npm ci}, {@code pnpm install}, ...). Up to date while
 * {@code package.json} and the lock file are unchanged and {@code node_modules} exists.
 */
public abstract class MustacheReactInstallTask extends DefaultTask implements NodeSettings {

    /** @return the frontend directory */
    @Internal
    public abstract DirectoryProperty getFrontendDir();

    /** @return {@code package.json} and the lock files that exist */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getPackageFiles();

    /** @return the package manager setting */
    @Input
    public abstract Property<String> getPackageManager();

    /** @return a file recording the last install */
    @OutputFile
    public abstract RegularFileProperty getStamp();

    /**
     * Runs the install.
     *
     * @throws IOException if writing the stamp fails
     */
    @TaskAction
    public void install() throws IOException {
        try {
            new FrontendBuild(getFrontendDir().get().getAsFile().toPath(), node(getLogger()), NodeSettings.log(getLogger()))
                    .install(getPackageManager().get());
        } catch (IOException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
        File stamp = getStamp().get().getAsFile();
        Files.createDirectories(stamp.getParentFile().toPath());
        Files.write(stamp.toPath(), "installed\n".getBytes(StandardCharsets.UTF_8));
    }
}
