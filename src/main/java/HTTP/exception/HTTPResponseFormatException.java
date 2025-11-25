package HTTP.exception;

public class HTTPResponseFormatException extends Exception {
    public HTTPResponseFormatException(String message) {
        super(message);
    }
    public HTTPResponseFormatException() {
        super();
    }
}
