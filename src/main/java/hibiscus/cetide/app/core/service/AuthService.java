package hibiscus.cetide.app.core.service;

import hibiscus.cetide.app.core.model.LoginRequest;
import hibiscus.cetide.app.core.model.LoginResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface AuthService {

    Map<String, Object> login(String username, String password);

    boolean validateToken(String token);

    String getUsernameFromToken(String token);

    LoginResponse login(LoginRequest request);

    void logout();

    boolean isAuthenticated();

    String getCurrentUsername();
} 