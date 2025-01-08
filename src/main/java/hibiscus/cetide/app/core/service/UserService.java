package hibiscus.cetide.app.core.service;

import hibiscus.cetide.app.core.model.User;
import hibiscus.cetide.app.core.util.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;
    
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockedUsers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 初始化管理员用户
        String adminPassword = PasswordUtils.hashPassword("admin123");
        Set<String> stringSet = new HashSet<>();
        stringSet.add("admin");
        User adminUser = new User("admin", adminPassword, stringSet);
        users.put("admin", adminUser);
        log.info("初始化管理员用户完成");
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        if (user == null) {
            log.warn("用户不存在: {}", username);
            return false;
        }

        // 检查用户是否被锁定
        if (isUserLocked(username)) {
            log.warn("用户已被锁定: {}", username);
            return false;
        }

        // 验证密码
        boolean isValid = PasswordUtils.verifyPassword(password, user.getPassword());
        if (isValid) {
            user.resetLoginAttempts();
            user.setLastLoginTime(LocalDateTime.now());
            log.info("用户登录成功: {}", username);
        } else {
            user.incrementLoginAttempts();
            log.warn("密码错误，用户: {}, 失败次数: {}", username, user.getLoginAttempts());
            
            // 检查是否需要锁定用户
            if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
                lockUser(username);
            }
        }

        return isValid;
    }

    private boolean isUserLocked(String username) {
        LocalDateTime lockTime = lockedUsers.get(username);
        if (lockTime == null) {
            return false;
        }

        if (lockTime.plusMinutes(LOCK_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
            // 锁定时间已过，解除锁定
            lockedUsers.remove(username);
            User user = users.get(username);
            if (user != null) {
                user.resetLoginAttempts();
            }
            return false;
        }

        return true;
    }

    private void lockUser(String username) {
        lockedUsers.put(username, LocalDateTime.now());
        log.warn("用户已被锁定: {}", username);
    }

    public Set<String> getUserRoles(String username) {
        User user = users.get(username);
        return user != null ? user.getRoles() : Collections.emptySet();
    }
} 