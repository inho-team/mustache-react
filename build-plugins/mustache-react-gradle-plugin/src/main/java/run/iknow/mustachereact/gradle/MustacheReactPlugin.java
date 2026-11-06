package run.iknow.mustachereact.gradle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.process.CommandLineArgumentProvider;

import run.iknow.mustachereact.build.NodeInstaller;
import run.iknow.mustachereact.build.PackageManager;

/**
 * {@code id("run.iknow.mustache-react")}: builds {@code src/main/frontend} into the application resources and
 * generates TypeScript types from the Java classes.
 *
 * <ul>
 *   <li>{@code mustacheReactInstall}: downloads Node.js (into {@code .mustache-react/node}) and installs the frontend
 *       dependencies.</li>
 *   <li>{@code mustacheReactBuild}: runs the frontend build; {@code processResources} picks up its output.</li>
 *   <li>{@code compileJava} runs {@code mustache-react-processor}, which writes
 *       {@code src/main/frontend/generated/types.ts}.</li>
 *   <li>{@code bootRun} and {@code run} tell the application where the frontend and Node.js are, for the dev server.</li>
 * </ul>
 */
public class MustacheReactPlugin implements Plugin<Project> {

    /** Group of the tasks. */
    public static final String GROUP = "mustache-react";

    /** Skips the frontend build: {@code -PmustacheReact.skipFrontend}. */
    public static final String SKIP_FRONTEND_PROPERTY = "mustacheReact.skipFrontend";

    @Override
    public void apply(Project project) {
        MustacheReactExtension extension = project.getExtensions().create("mustacheReact", MustacheReactExtension.class);
        extension.getFrontendDir().convention(project.getLayout().getProjectDirectory().dir("src/main/frontend"));
        extension.getPublicPath().convention("/mustache-react/");
        extension.getFrontendBuild().convention(project.getProviders().gradleProperty(SKIP_FRONTEND_PROPERTY)
                .map(value -> "false".equalsIgnoreCase(value)).orElse(true));
        extension.getInstall().convention(true);
        extension.getPackageManager().convention("auto");
        extension.getNodeDownload().convention(true);
        extension.getNodeVersion().convention(NodeInstaller.DEFAULT_VERSION);
        extension.getNodeDistributionUrl().convention(NodeInstaller.DEFAULT_DISTRIBUTION_URL);
        extension.getNodeInstallDir().convention(project.getLayout().getProjectDirectory().dir(".mustache-react/node"));
        extension.getTypes().convention(true);
        extension.getTypesFile().convention(extension.getFrontendDir().file("generated/types.ts"));

        Provider<Directory> frontendDir = extension.getFrontendDir();

        TaskProvider<MustacheReactInstallTask> install = project.getTasks().register("mustacheReactInstall",
                MustacheReactInstallTask.class, task -> {
                    task.setGroup(GROUP);
                    task.setDescription("Installs the frontend dependencies.");
                    configureNode(task, extension);
                    task.getFrontendDir().set(frontendDir);
                    task.getPackageFiles().from(frontendDir.map(dir -> project.files(
                            PackageManager.inputFiles().stream().map(dir::file).toArray())
                            .filter(File::isFile)));
                    task.getPackageManager().set(extension.getPackageManager());
                    task.getStamp().set(project.getLayout().getBuildDirectory().file("mustache-react/install.stamp"));
                    task.onlyIf(t -> extension.getInstall().get() && extension.getFrontendBuild().get());
                    task.getOutputs().upToDateWhen(t ->
                            new File(task.getFrontendDir().get().getAsFile(), "node_modules").isDirectory());
                });

        TaskProvider<MustacheReactBuildTask> build = project.getTasks().register("mustacheReactBuild",
                MustacheReactBuildTask.class, task -> {
                    task.setGroup(GROUP);
                    task.setDescription("Builds the frontend into the application resources.");
                    configureNode(task, extension);
                    task.getFrontendDir().set(frontendDir);
                    task.getSources().from(frontendDir.map(dir -> project.fileTree(dir, tree ->
                            tree.exclude("node_modules/**", "dist/**", ".vite/**"))));
                    task.getPublicPath().set(extension.getPublicPath());
                    task.getOutputDir().set(project.getLayout().getBuildDirectory().dir("mustache-react/resources"));
                    task.dependsOn(install);
                    task.onlyIf(t -> extension.getFrontendBuild().get());
                });

        project.getPluginManager().withPlugin("java", plugin -> {
            project.getTasks().named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, ProcessResources.class, task ->
                    task.from(build.flatMap(MustacheReactBuildTask::getOutputDir)));
            configureTypes(project, extension, build);
        });

        project.getTasks().withType(JavaExec.class).configureEach(task -> task.getJvmArgumentProviders().add(
                (CommandLineArgumentProvider) () -> Arrays.asList(
                        "-Dmustache-react.frontend-dir=" + frontendDir.get().getAsFile().getAbsolutePath(),
                        "-Dmustache-react.dev-server.node-install-dir="
                                + (extension.getNodeDownload().get()
                                        ? extension.getNodeInstallDir().get().getAsFile().getAbsolutePath() : ""))));
    }

    private static void configureNode(NodeSettings task, MustacheReactExtension extension) {
        task.getNodeDownload().set(extension.getNodeDownload());
        task.getNodeVersion().set(extension.getNodeVersion());
        task.getNodeDistributionUrl().set(extension.getNodeDistributionUrl());
        task.getNodeInstallDir().set(extension.getNodeInstallDir());
    }

    private static void configureTypes(Project project, MustacheReactExtension extension,
            TaskProvider<MustacheReactBuildTask> build) {
        project.afterEvaluate(p -> {
            if (!extension.getTypes().get()) {
                return;
            }
            Object processor = extension.getProcessorDependency() != null ? extension.getProcessorDependency()
                    : "run.iknow:mustache-react-processor:" + pluginVersion();
            p.getDependencies().add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, processor);
        });
        project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile.class, task -> {
            task.getOptions().getCompilerArgumentProviders().add(() -> extension.getTypes().get()
                    ? Collections.singletonList("-Amustachereact.typesFile="
                            + extension.getTypesFile().get().getAsFile().getAbsolutePath())
                    : Collections.<String>emptyList());
            // Lets Gradle re-create the file when it is deleted; the frontend build reads it.
            task.getOutputs().file(extension.getTypesFile()).withPropertyName("mustacheReactTypes").optional();
        });
        // The generated types are part of the frontend sources.
        build.configure(task -> task.mustRunAfter(JavaPlugin.COMPILE_JAVA_TASK_NAME));
    }

    static String pluginVersion() {
        try (InputStream in = MustacheReactPlugin.class.getResourceAsStream("/META-INF/mustache-react/gradle-plugin.properties")) {
            Properties properties = new Properties();
            if (in != null) {
                properties.load(in);
            }
            return properties.getProperty("version", "unknown");
        } catch (IOException ex) {
            return "unknown";
        }
    }
}
