package hibiscus.cetide.app.module.control;

public class UserRequest {
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "UserRequest{" +
                "username='" + username + '\'' +
                '}';
    }
}
