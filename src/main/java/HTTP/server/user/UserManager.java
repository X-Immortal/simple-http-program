package HTTP.server.user;

import HTTP.server.user.exception.PasswordFormatException;
import HTTP.server.user.exception.UserNotExistsException;
import HTTP.server.user.exception.UsernameFormatException;

import java.io.*;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private UserManager() {}

    private final static ConcurrentHashMap<String, User> currentUsers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, User> users;
    private static String USERS_PATH;

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
            users = new ConcurrentHashMap<>();
        } else {
            loadUsers();
        }
    }

    public static boolean isLoggedIn(String username) throws UserNotExistsException {
        if (!users.containsKey(username)) {
            throw new UserNotExistsException();
        }

        return currentUsers.containsKey(username);
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

    public static boolean login(String username) throws UserNotExistsException {
        if (!users.containsKey(username)) {
            throw new UserNotExistsException();
        }

        if (currentUsers.containsKey(username)) {
            return false;
        }

        currentUsers.put(username, users.get(username));
        return true;
    }

    public static boolean logout(String username) throws UserNotExistsException {
        if (!users.containsKey(username)) {
            throw new UserNotExistsException();
        }

        if (!currentUsers.containsKey(username)) {
            return false;
        }

        currentUsers.remove(username);
        return true;
    }

    private static void loadUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USERS_PATH))) {
            users = (ConcurrentHashMap<String, User>) ois.readObject();
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
}
