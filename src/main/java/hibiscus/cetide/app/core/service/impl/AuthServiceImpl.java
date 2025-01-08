package hibiscus.cetide.app.core.service.impl;

import hibiscus.cetide.app.config.HibiscusAdminConfig;
import hibiscus.cetide.app.core.model.LoginRequest;
import hibiscus.cetide.app.core.model.LoginResponse;
import hibiscus.cetide.app.core.service.AuthService;
import hibiscus.cetide.app.core.util.JwtUtil;
import hibiscus.cetide.app.core.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务实现类
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String AUTH_ERROR_MESSAGE = "用户名或密码错误";
    private static final String SYSTEM_ERROR_MESSAGE = "登录服务暂时不可用，请稍后重试";

    // 存储用户会话信息
    private static final Map<String, String> userSessions = new ConcurrentHashMap<>();
    
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final HibiscusAdminConfig adminConfig;
    @Autowired
    public AuthServiceImpl(JwtUtil jwtUtil, PasswordUtil passwordUtil, HibiscusAdminConfig adminConfig) {
        this.jwtUtil = jwtUtil;
        this.passwordUtil = passwordUtil;
        this.adminConfig = adminConfig;
    }

    /**
     * 处理用户登录请求
     *
     * @param request 登录请求DTO
     * @return 登录响应DTO
     * @throws RuntimeException 认证失败时抛出
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        validateLoginRequest(request);
        try {
            // 验证用户名密码
            if (!authenticateUser(request.getUsername(), request.getPassword())) {
                log.error("登录失败 - 凭证错误: username={}", request.getUsername());
                throw new RuntimeException(AUTH_ERROR_MESSAGE);
            }

            // 生成token
            String token = jwtUtil.generateToken(request.getUsername());
            // 保存会话信息
            userSessions.put(token, request.getUsername());
            
            return createLoginResponse(request.getUsername(), token);
        } catch (RuntimeException e) {
            log.error("登录失败: username={}, error={}", request.getUsername(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("登录过程发生异常: username={}, error={}", request.getUsername(), e.getMessage(), e);
            throw new RuntimeException(SYSTEM_ERROR_MESSAGE);
        }
    }

    /**
     * 用户注销
     */
    @Override
    public void logout() {
        String token = getCurrentToken();
        if (token != null) {
            String username = userSessions.remove(token);
            if (username != null) {
                log.info("用户注销: {}", username);
            }
        }
    }

    /**
     * 检查用户是否已认证
     */
    @Override
    public boolean isAuthenticated() {
        String token = getCurrentToken();
        return token != null && userSessions.containsKey(token);
    }

    /**
     * 获取当前登录用户名
     */
    @Override
    public String getCurrentUsername() {
        String token = getCurrentToken();
        return token != null ? userSessions.get(token) : null;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        // 为了向后兼容，保留此方法
        return Collections.emptyMap();
    }

    @Override
    public boolean validateToken(String token) {
        return token != null && !token.trim().isEmpty() 
            && userSessions.containsKey(token) 
            && jwtUtil.validateToken(token);
    }

    @Override
    public String getUsernameFromToken(String token) {
        if (token != null && !token.trim().isEmpty() && userSessions.containsKey(token)) {
            return userSessions.get(token);
        }
        return "";
    }

    // 私有辅助方法

    private void validateLoginRequest(LoginRequest request) {
        Assert.notNull(request, "登录请求不能为空");
        Assert.hasText(request.getUsername(), "用户名不能为空");
        Assert.hasText(request.getPassword(), "密码不能为空");
    }

    private boolean authenticateUser(String username, String password) {
        System.out.println(adminConfig);
        if (adminConfig.getAdmins() == null || adminConfig.getAdmins().isEmpty()){
            HibiscusAdminConfig.AdminUser defaultAdmin = new HibiscusAdminConfig.AdminUser();
            defaultAdmin.setUsername("admin");
            defaultAdmin.setPassword("admin");
            adminConfig.getAdmins().add(defaultAdmin);
        }
        System.out.println(adminConfig);
        return adminConfig.getAdmins().stream()
                .anyMatch(admin ->
                        admin.getUsername().equals(username) &&
                                admin.getPassword().equals(password)
                );
    }

    private LoginResponse createLoginResponse(String username, String token) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsername(username);
        response.setRole(ROLE_ADMIN);
        log.info("登录成功: username={}", username);
        return response;
    }

    private String getCurrentToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String bearerToken = request.getHeader("Authorization");
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }
        return null;
    }
}