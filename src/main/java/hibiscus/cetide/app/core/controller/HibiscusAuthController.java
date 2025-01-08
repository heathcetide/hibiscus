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

    @GetMapping("/login")
    public String login() {
        if (authService.isAuthenticated()) {
            return "redirect:/api/hibiscus/code/backstage";
        }
        return "login";
    }

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