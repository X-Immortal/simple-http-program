package HTTP.server.user;

import HTTP.server.user.exception.PasswordFormatException;
import HTTP.server.user.exception.UsernameFormatException;

import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

public class User implements Serializable {
    private static final long serialVersionUID = 3435220335721232368L;
    // 用户名只能包含中文、英文字母、数字、下划线，且不能以数字开头，长度为4-16
    private static final String USERNAME_FORMAT = "([\\u4E00-\\u9FA5]|[\\w&&\\D])[\\u4E00-\\u9FA5\\w]{3,15}";
    // 密码只能且必须包含大小写字母和数字，长度为8-20
    // .*表示全局扫描，(?=.*[])表示检查整个串中是否包含[]中的内容
    private static final String PASSWORD_FORMAT = "(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[\\w&&[^_]]{8,20}";
    private final String username;
    private String password;
    private final int userid;

    public User(String username, String password) throws UsernameFormatException, PasswordFormatException {
        if (!username.matches(USERNAME_FORMAT)) {
            throw new UsernameFormatException();
        }
        if (!password.matches(PASSWORD_FORMAT)) {
            throw new PasswordFormatException();
        }
        this.username = username;
        this.password = password;
        userid = new Random().nextInt();
    }

    /**
     * 获取
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * 获取
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置
     * @param password
     */
    public void setPassword(String password) throws PasswordFormatException {
        if (!password.matches(PASSWORD_FORMAT)) {
            throw new PasswordFormatException();
        }
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User)) return false;
        return Objects.equals(username, ((User) o).username) && Objects.equals(password, ((User) o).password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, userid);
    }
}
