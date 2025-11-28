package HTTP.server.user;

import java.io.*;
import java.util.HashSet;
import java.util.stream.Collectors;

public class UserManager {
    private UserManager() {}

    private static User currentUser;
    private static HashSet<User> users;
    private static HashSet<String> usernames;
    private static final String USERS_PATH = "09PuzzleGame\\data\\users";

    static {
        File usersFile = new File(USERS_PATH);
        if (!usersFile.exists()) {
            new File(usersFile.getParent()).mkdirs();
            try {
                usersFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            users = new HashSet<>();
            usernames = new HashSet<>();
            setUsers(users);
        } else {
            readUsers();
        }
    }

    public static HashSet<User> getUsers() {
        return users;
    }

    public static HashSet<String> getUsernames() {
        return usernames;
    }

    public static void setUsers(HashSet<User> users) {
        UserManager.users = users;
        readUsernames();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_PATH))) {
            oos.writeObject(users);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    private static void readUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USERS_PATH))) {
            users = (HashSet<User>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        readUsernames();
    }

    private static void readUsernames() {
        usernames = users.stream().map(User::getUsername).collect(Collectors.toCollection(HashSet::new));
    }
}
