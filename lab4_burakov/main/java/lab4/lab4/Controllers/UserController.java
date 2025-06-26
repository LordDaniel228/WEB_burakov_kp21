package lab4.lab4.Controllers;
import lab4.lab4.CasdoorProperties;
import lab4.lab4.JwtDecoder;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final JwtDecoder jwtDecoder;

    public UserController(CasdoorProperties casdoorProperties) {
        this.jwtDecoder = new JwtDecoder(casdoorProperties);
    }

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(@CookieValue(value = "access_token", required = false) String accessToken) {
        if (accessToken == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Access token is missing"));
        }

        try {
            logger.info("Decoding JWT token");
            Claims claims = jwtDecoder.decodeToken(accessToken);

            String userId = claims.getSubject();
            String username = claims.get("name", String.class);

            logger.debug("Decoded userId: {}, username: {}", userId, username);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", userId);
            userInfo.put("username", username);

            return ResponseEntity.ok(userInfo);

        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            logger.error("Malformed token: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Malformed token", "details", e.getMessage()));
        } catch (org.json.JSONException e) {
            logger.error("Invalid token payload: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid token payload", "details", e.getMessage()));
        } catch (Exception e) {
            logger.error("Invalid or expired token: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token", "details", e.getMessage()));
        }
    }
}
