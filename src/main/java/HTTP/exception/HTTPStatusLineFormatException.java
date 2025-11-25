package HTTP.exception;

public class HTTPStatusLineFormatException extends HTTPResponseFormatException {
    public HTTPStatusLineFormatException(String message) {
        super(message);
    }
    public HTTPStatusLineFormatException() {
        super();
    }
}
