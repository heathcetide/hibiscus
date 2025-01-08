package hibiscus.cetide.app.core.controller;

import hibiscus.cetide.app.core.model.LoginRequest;
import hibiscus.cetide.app.core.model.LoginResponse;
import hibiscus.cetide.app.core.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/hibiscus/auth")
public class HibiscusAuthController {

    private static final Logger log = LoggerFactory.getLogger(HibiscusAuthController.class);
    
    private final AuthService authService;

    @Autowired
    public HibiscusAuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 处理登录页面
     *
     * @return 如果已认证，重定向到后台页面；否则返回登录页面。
     */
    @GetMapping("/login")
    public String login() {
        if (authService.isAuthenticated()) {
            return "redirect:/api/hibiscus/code/backstage";
        }
        return "login";
    }

    /**
     * 处理登录操作
     *
     * @param request 登录请求参数（用户名、密码等）
     * @return 登录成功时返回包含用户信息的响应，失败时返回错误信息。
     */
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // 设置默认的remember值为false
            if (request.isRemember() == null) {
                request.setRemember(false);
            }
            
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("登录失败: username={}, error={}", request.getUsername(), e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "登录失败");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 处理注销操作
     *
     * @return 注销成功返回空响应，失败返回错误信息。
     */
    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<?> logout() {
        try {
            authService.logout();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("注销失败: error={}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "注销失败");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 处理检查用户认证状态
     *
     * @return 返回用户是否已认证的信息。
     */
    @GetMapping("/check")
    @ResponseBody
    public ResponseEntity<?> checkAuth() {
        boolean isAuthenticated = authService.isAuthenticated();
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", isAuthenticated);
        if (isAuthenticated) {
            response.put("username", authService.getCurrentUsername());
        }
        return ResponseEntity.ok(response);
    }
} 