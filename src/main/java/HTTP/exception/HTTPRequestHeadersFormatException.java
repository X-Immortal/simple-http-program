package HTTP.exception;

public class HTTPRequestHeadersFormatException extends HTTPRequestFormatException {
    public HTTPRequestHeadersFormatException(String message) {
        super(message);
    }
    public HTTPRequestHeadersFormatException() {
        super();
    }
}
