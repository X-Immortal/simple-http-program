package HTTP.exception;

public class HTTPMethodNotAllowedException extends Exception {
    public HTTPMethodNotAllowedException(String message) {
        super(message);
    }
    public HTTPMethodNotAllowedException() {
        super();
    }
}
