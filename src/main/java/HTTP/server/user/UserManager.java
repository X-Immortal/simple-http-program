package HTTP.server.user;

import HTTP.server.user.exception.PasswordException;
import HTTP.server.user.exception.PasswordFormatException;
import HTTP.server.user.exception.UserNotExistsException;
import HTTP.server.user.exception.UsernameFormatException;

import java.io.*;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private UserManager() {}

    private final static ConcurrentHashMap<String, String> currentUsers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, User> users;
    private static String USERS_PATH;
    private static User root;
    private static final String ROOT_TOKEN = "80aea92e-b9c2-4efd-8f18-a2c64d9b737f";

    static {
        getUserPath();

        File usersFile = new File(USERS_PATH);
        if (!usersFile.exists()) {
            new File(usersFile.getParent()).mkdirs();
            try {
                usersFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                root = new User("root", "Root1234");
            } catch (UsernameFormatException | PasswordFormatException e) {
                throw new RuntimeException(e);
            }
            users = new ConcurrentHashMap<>();
            users.put("root", root);
            saveUsers();
        } else {
            loadUsers();
        }
    }

    public static boolean isLoggedIn(String token) {
        return currentUsers.containsKey(token);
    }

    public static boolean register(String username, String password) throws PasswordFormatException, UsernameFormatException {
        if (users.containsKey(username)) {
            return false;
        }
        User user = new User(username, password);
        users.put(username, user);
        saveUsers();
        return true;
    }

    public static String login(String username, String password) throws UserNotExistsException, PasswordException {
        if (!users.containsKey(username)) {
            throw new UserNotExistsException();
        }

        if (currentUsers.containsValue(username)) {
            return null;
        }

        if (!users.get(username).getPassword().equals(password)) {
            throw new PasswordException("password error");
        }

        String token = getToken();
        currentUsers.put(token, username);
        return token;
    }

    public static boolean logout(String token) {
        if (!currentUsers.containsKey(token)) {
            return false;
        }

        currentUsers.remove(token);
        return true;
    }

    public static String getUserDirByName(String username) {
        if (!users.containsKey(username)) {
            return null;
        }
        User user = users.get(username);
        return String.format("%08x", user.hashCode());
    }

    public static String getUserDirByToken(String token) {
        if (!currentUsers.containsKey(token)) {
            return null;
        }
        String username = currentUsers.get(token);
        User user = users.get(username);
        return String.format("%08x", user.hashCode());
    }

    public static String getUsername(String token) {
        if (!isLoggedIn(token)) {
            return null;
        }
        return currentUsers.get(token);
    }

    public static String getRootToken() {
        return ROOT_TOKEN;
    }

    private static void loadUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USERS_PATH))) {
            users = (ConcurrentHashMap<String, User>) ois.readObject();
            root = users.get("root");
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_PATH))) {
            oos.writeObject(users);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void getUserPath() {
        String workingDir = System.getProperty("user.dir");
        StringJoiner joiner = new StringJoiner(File.separator);
        joiner.add(workingDir).add(".data").add("users");
        USERS_PATH = joiner.toString();
    }

    private static String getToken() {
        return UUID.randomUUID().toString();
    }
}
