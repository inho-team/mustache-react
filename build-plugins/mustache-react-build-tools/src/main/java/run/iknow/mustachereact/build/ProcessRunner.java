package run.iknow.mustachereact.build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Runs a command in a directory and forwards its output to the build log.
 */
final class ProcessRunner {

    private ProcessRunner() {
    }

    static void run(List<String> command, Path directory, Map<String, String> environment, BuildLog log)
            throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
        builder.environment().putAll(environment);
        Process process;
        try {
            process = builder.start();
        } catch (IOException ex) {
            throw new IOException("Could not run " + command.get(0) + ". Is it installed and on the PATH? ("
                    + ex.getMessage() + ")", ex);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        }
        try {
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IOException("'" + String.join(" ", command) + "' failed with exit code " + exit
                        + " (in " + directory + ")");
            }
        } catch (InterruptedException ex) {
            process.destroy();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while running " + command.get(0), ex);
        }
    }
}
