package run.iknow.mustachereact.examples.basic.auth;

public class LoginPage {

    private final boolean error;

    public LoginPage(boolean error) {
        this.error = error;
    }

    /** Whether the last sign-in attempt failed. */
    public boolean isError() {
        return error;
    }
}
