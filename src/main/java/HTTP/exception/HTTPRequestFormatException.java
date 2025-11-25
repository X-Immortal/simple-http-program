package HTTP.exception;

public class HTTPRequestFormatException extends Exception {
    public HTTPRequestFormatException(String message) {
        super(message);
    }
    public HTTPRequestFormatException() {
        super();
    }
}
