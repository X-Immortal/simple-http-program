package HTTP.server.user.exception;

public class UserNotExistsException extends Exception {
    public UserNotExistsException(String message) {
        super(message);
    }
    public UserNotExistsException() {
        super();
    }
}
