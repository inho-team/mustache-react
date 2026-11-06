package run.iknow.mustachereact.processor;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Compiles sample sources with the processor and returns the TypeScript it wrote. */
final class Compilation {

    final boolean success;
    final String output;
    final String diagnostics;
    final Path typesFile;

    private Compilation(boolean success, String output, String diagnostics, Path typesFile) {
        this.success = success;
        this.output = output;
        this.diagnostics = diagnostics;
        this.typesFile = typesFile;
    }

    static Compilation compile(Path workDir, Map<String, String> sources) throws IOException {
        Path src = Files.createDirectories(workDir.resolve("src"));
        Path classes = Files.createDirectories(workDir.resolve("classes"));
        Path typesFile = workDir.resolve("frontend/generated/types.ts");
        List<File> files = new ArrayList<>();
        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path file = src.resolve(source.getKey().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.write(file, source.getValue().getBytes(StandardCharsets.UTF_8));
            files.add(file.toFile());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StringWriter diagnostics = new StringWriter();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(files);
            List<String> options = new ArrayList<>(Arrays.asList(
                    "-proc:only",
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classes.toString(),
                    "-A" + TypeScriptProcessor.OPTION_TYPES_FILE + "=" + typesFile));
            JavaCompiler.CompilationTask task = compiler.getTask(diagnostics, fileManager, null, options, null, units);
            task.setProcessors(java.util.Collections.singletonList(new TypeScriptProcessor()));
            boolean success = task.call();
            String output = Files.exists(typesFile) ? new String(Files.readAllBytes(typesFile), StandardCharsets.UTF_8) : null;
            return new Compilation(success, output, diagnostics.toString(), typesFile);
        }
    }
}
