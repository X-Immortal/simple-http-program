package HTTP.server.user.exception;


public class UsernameFormatException extends Exception {
    private static final long serialVersionUID = 8037195726021744380L;

    public UsernameFormatException(String message) {
        super(message);
    }

    public UsernameFormatException() {
        super();
    }
}
