package lab4.lab4.Controllers;

import lab4.lab4.CasdoorProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final CasdoorProperties casdoorProperties;

    public AuthController(CasdoorProperties casdoorProperties) {
        this.casdoorProperties = casdoorProperties;
    }

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
    System.out.println("Login requested");

    String state = UUID.randomUUID().toString();
    String redirectUri = casdoorProperties.getRedirectUri();

    
    String authorizeUrl = String.format("%s%s?client_id=%s&response_type=code&redirect_uri=%s&scope=openid profile email&state=%s&nonce=%s",
            casdoorProperties.getConnectEndpoint(),
            casdoorProperties.getLoginEndpoint(),
            casdoorProperties.getConnectClientId(),
            redirectUri,  
            state,
            UUID.randomUUID()
    );

    System.out.println("Authorize URL: " + authorizeUrl);
    response.sendRedirect(authorizeUrl);
}

    @SuppressWarnings("rawtypes")
    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code,
    @RequestParam(required = false) String state,
    @RequestParam(required = false) String error,
    @RequestParam(required = false) String error_description,
    HttpServletRequest servletRequest, 
    HttpServletResponse response) throws IOException {
        
        System.out.println("Callback received. Full URL: " + servletRequest.getRequestURL() + "?" + servletRequest.getQueryString());
        
        if (error != null) {
            System.err.println("OAuth error: " + error + " - " + error_description);
            response.sendRedirect("/?error=oauth_error&message=" + error);
            return;
        }
        
        if (code == null) {
            System.err.println("Missing required 'code' parameter");
            response.sendRedirect("/?error=missing_code");
            return;
        }
        
        System.out.println("Got code: " + code);
        System.out.println("Got state: " + state);
        
        RestTemplate restTemplate = createRestTemplateWithSSLSupport();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", casdoorProperties.getRedirectUri());
        params.add("client_id", casdoorProperties.getConnectClientId());
    params.add("client_secret", casdoorProperties.getConnectClientSecret());
    
    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers); 
    ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
        casdoorProperties.getConnectEndpoint() + casdoorProperties.getTokenEndpoint(),
        requestEntity,
        Map.class
        );
        
        if (tokenResponse.getStatusCode().is2xxSuccessful()) {
            String accessToken = (String) tokenResponse.getBody().get("access_token");
            
            if (accessToken != null) {
                Cookie accessTokenCookie = new Cookie("access_token", accessToken);
                accessTokenCookie.setPath("/");
                accessTokenCookie.setHttpOnly(true);
                accessTokenCookie.setSecure(true);
                response.addCookie(accessTokenCookie);
                System.out.println("Access token stored in cookie");
            } else {
                System.err.println("Access token not found in response");
            }
        } else {
            System.err.println("Failed to retrieve access token: " + tokenResponse.getStatusCode());
        }
        
        response.sendRedirect("/");
    }
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getUserStatus(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        Cookie[] cookies = request.getCookies();
        boolean loggedIn = false;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    loggedIn = true;
                    break;
                }
            }
        }

        response.put("loggedIn", loggedIn);
        return ResponseEntity.ok(response);
    }

    private RestTemplate createRestTemplateWithSSLSupport() {
    try {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((_, _) -> true);

        return new RestTemplate();
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
}
