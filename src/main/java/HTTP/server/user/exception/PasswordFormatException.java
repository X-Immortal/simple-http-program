package HTTP.server.user.exception;


public class PasswordFormatException extends Exception {
    private static final long serialVersionUID = 1726912274235159993L;

    public PasswordFormatException(String message) {
        super(message);
    }

    public PasswordFormatException() {
        super();
    }
}
