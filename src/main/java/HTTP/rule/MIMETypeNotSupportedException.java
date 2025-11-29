package HTTP.rule;

public class MIMETypeNotSupportedException extends Exception {
    public MIMETypeNotSupportedException(String message) {
        super(message);
    }
    public MIMETypeNotSupportedException() {
        super();
    }
}
