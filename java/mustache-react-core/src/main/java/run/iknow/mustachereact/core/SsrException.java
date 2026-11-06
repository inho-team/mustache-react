package run.iknow.mustachereact.core;

/** Server-side rendering failed or is unavailable. */
public class SsrException extends Exception {

    private static final long serialVersionUID = 1L;

    /** @param message the reason */
    public SsrException(String message) {
        super(message);
    }

    /**
     * @param message the reason
     * @param cause the cause
     */
    public SsrException(String message, Throwable cause) {
        super(message, cause);
    }
}
