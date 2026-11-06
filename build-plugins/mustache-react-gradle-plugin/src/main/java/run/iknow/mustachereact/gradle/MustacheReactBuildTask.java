package run.iknow.mustachereact.gradle;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import run.iknow.mustachereact.build.FrontendBuild;

/**
 * Builds the frontend with {@code mustache-react-vite build}. The output directory is added to the resources, so the
 * files end up in {@code static/mustache-react/} of the jar, where Spring Boot serves them.
 */
@CacheableTask
public abstract class MustacheReactBuildTask extends DefaultTask implements NodeSettings {

    /** @return the frontend directory */
    @Internal
    public abstract DirectoryProperty getFrontendDir();

    /** @return the frontend sources, without {@code node_modules} */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /** @return the public path */
    @Input
    public abstract Property<String> getPublicPath();

    /** @return the resources directory receiving {@code static/<public path>/} */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /** @return file operations */
    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    /** Runs the build. */
    @TaskAction
    public void build() {
        File output = getOutputDir().get().getAsFile();
        getFileSystemOperations().delete(spec -> spec.delete(output));
        String publicPath = getPublicPath().get();
        File staticDir = new File(new File(output, "static"), publicPath.replaceAll("^/+|/+$", ""));
        try {
            new FrontendBuild(getFrontendDir().get().getAsFile().toPath(), node(getLogger()), NodeSettings.log(getLogger()))
                    .build(staticDir.toPath(), publicPath);
        } catch (IOException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
    }
}
