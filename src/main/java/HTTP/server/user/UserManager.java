package HTTP.server.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private static final Map<String, User> users = new ConcurrentHashMap<>();

    private static final UserManager manager = new UserManager();

    private UserManager() {}
}
