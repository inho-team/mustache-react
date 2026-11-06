package run.iknow.mustachereact.build;

/**
 * Logging of the build tool running the integration (Gradle logger, Maven log).
 */
public interface BuildLog {

    /**
     * @param message an informational message
     */
    void info(String message);

    /**
     * @param message a warning
     */
    void warn(String message);

    /** Writes to standard output and error, for tests and simple callers. */
    BuildLog STANDARD = new BuildLog() {
        @Override
        public void info(String message) {
            System.out.println(message);
        }

        @Override
        public void warn(String message) {
            System.err.println(message);
        }
    };
}
