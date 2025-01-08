package hibiscus.cetide.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.ArrayList;

@Component
@ConfigurationProperties(prefix = "hibiscus.auth")
public class HibiscusAdminConfig {
    private List<AdminUser> admins = new ArrayList<>();

    public List<AdminUser> getAdmins() {
        return admins;
    }

    public void setAdmins(List<AdminUser> admins) {
        this.admins = admins;
    }

    public static class AdminUser {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String toString() {
            return "AdminUser{" +
                    "username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "HibiscusAdminConfig{" +
                "admins=" + admins +
                '}';
    }
}