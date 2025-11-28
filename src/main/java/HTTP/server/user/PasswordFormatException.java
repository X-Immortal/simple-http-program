package HTTP.server.user;

import java.io.Serial;

public class PasswordFormatException extends Exception {
    @Serial
    private static final long serialVersionUID = 1726912274235159993L;

    public PasswordFormatException(String message) {
        super(message);
    }

    public PasswordFormatException() {
        super();
    }
}
