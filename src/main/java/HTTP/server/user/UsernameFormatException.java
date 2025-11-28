package HTTP.server.user;

import java.io.Serial;

public class UsernameFormatException extends Exception {
    @Serial
    private static final long serialVersionUID = 8037195726021744380L;

    public UsernameFormatException(String message) {
        super(message);
    }

    public UsernameFormatException() {
        super();
    }
}
