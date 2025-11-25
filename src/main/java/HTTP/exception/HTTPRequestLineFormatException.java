package HTTP.exception;

public class HTTPRequestLineFormatException extends HTTPRequestFormatException {
    public HTTPRequestLineFormatException(String message) {
        super(message);
    }
    public HTTPRequestLineFormatException() {
        super();
    }
}
