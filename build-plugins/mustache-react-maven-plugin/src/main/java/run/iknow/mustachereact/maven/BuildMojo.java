package run.iknow.mustachereact.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import run.iknow.mustachereact.build.BuildLog;
import run.iknow.mustachereact.build.FrontendBuild;
import run.iknow.mustachereact.build.NodeInstaller;
import run.iknow.mustachereact.build.NodeRuntime;

/**
 * Builds {@code src/main/frontend} into the application resources ({@code static/mustache-react/} in the jar).
 * Downloads Node.js into {@code .mustache-react/node} and installs the frontend dependencies first, when
 * {@code package.json} or the lock file changed.
 *
 * <pre>{@code
 * <plugin>
 *   <groupId>run.iknow</groupId>
 *   <artifactId>mustache-react-maven-plugin</artifactId>
 *   <version>...</version>
 *   <executions><execution><goals><goal>build</goal></goals></execution></executions>
 * </plugin>
 * }</pre>
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class BuildMojo extends AbstractMojo {

    /** Frontend directory holding {@code package.json}. */
    @Parameter(defaultValue = "${project.basedir}/src/main/frontend", property = "mustacheReact.frontendDir")
    private File frontendDir;

    /** URL path Spring serves the assets from; must match {@code mustache-react.public-path}. */
    @Parameter(defaultValue = "/mustache-react/", property = "mustacheReact.publicPath")
    private String publicPath;

    /** Skips the frontend build, e.g. {@code -DmustacheReact.skipFrontend} when only the Java side is tested. */
    @Parameter(defaultValue = "false", property = "mustacheReact.skipFrontend")
    private boolean skipFrontend;

    /**
     * Whether to build the server-side rendering bundle too, for {@code mustache-react.ssr=auto|always}. Needs
     * {@code @mustache-react/server} in the frontend dependencies.
     */
    @Parameter(defaultValue = "false", property = "mustacheReact.ssr")
    private boolean ssr;

    /** Whether to install the frontend dependencies before building. */
    @Parameter(defaultValue = "true", property = "mustacheReact.install")
    private boolean install;

    /** {@code auto}, {@code npm}, {@code pnpm} or {@code yarn}. */
    @Parameter(defaultValue = "auto", property = "mustacheReact.packageManager")
    private String packageManager;

    /** Whether to download Node.js instead of using the one on the {@code PATH}. */
    @Parameter(defaultValue = "true", property = "mustacheReact.nodeDownload")
    private boolean nodeDownload;

    /** Node.js version to download. */
    @Parameter(defaultValue = NodeInstaller.DEFAULT_VERSION, property = "mustacheReact.nodeVersion")
    private String nodeVersion;

    /** Where to download Node.js from. */
    @Parameter(defaultValue = NodeInstaller.DEFAULT_DISTRIBUTION_URL, property = "mustacheReact.nodeDistributionUrl")
    private String nodeDistributionUrl;

    /** Where downloaded Node.js versions are kept. Spring's dev server auto-start looks here too. */
    @Parameter(defaultValue = "${project.basedir}/.mustache-react/node", property = "mustacheReact.nodeInstallDir")
    private File nodeInstallDir;

    /** Resources directory receiving {@code static/<public path>/}. */
    @Parameter(defaultValue = "${project.build.directory}/mustache-react")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        if (skipFrontend) {
            getLog().info("mustache-react: frontend build skipped");
            return;
        }
        BuildLog log = log(getLog());
        try {
            NodeRuntime node = nodeDownload
                    ? new NodeInstaller(nodeInstallDir.toPath(), nodeDistributionUrl, log).install(nodeVersion)
                    : NodeRuntime.fromPath();
            FrontendBuild build = new FrontendBuild(frontendDir.toPath(), node, log);
            if (install) {
                build.installIfChanged(packageManager);
            }
            File staticDir = new File(new File(outputDirectory, "static"), publicPath.replaceAll("^/+|/+$", ""));
            deleteRecursively(outputDirectory);
            build.build(staticDir.toPath(), publicPath,
                    ssr ? new File(outputDirectory, FrontendBuild.SSR_RESOURCE_DIR).toPath() : null);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        Resource resource = new Resource();
        resource.setDirectory(outputDirectory.getAbsolutePath());
        project.addResource(resource);
    }

    private static void deleteRecursively(File file) throws IOException {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (file.exists() && !file.delete()) {
            throw new IOException("Could not delete " + file);
        }
    }

    private static BuildLog log(Log log) {
        return new BuildLog() {
            @Override
            public void info(String message) {
                log.info(message);
            }

            @Override
            public void warn(String message) {
                log.warn(message);
            }
        };
    }
}
