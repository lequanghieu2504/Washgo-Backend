package com.example.washgo.controller;

import com.example.washgo.dtos.*;
import com.example.washgo.model.RefreshToken;
import com.example.washgo.model.UserAccount;
import com.example.washgo.model.UserInformation;
import com.example.washgo.enums.UserRole;
import com.example.washgo.security.JwtService; // <-- import your JwtService
import com.example.washgo.service.AuthService;
import com.example.washgo.service.GoogleOAuthService;
import com.example.washgo.service.RefreshTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService; // ✅ Inject JwtService
    private final RefreshTokenService refreshTokenService; // ✅ Inject RefreshTokenService
    private final GoogleOAuthService googleOAuthService; // ✅ Inject GoogleOAuthService
    public AuthController(AuthService authService, JwtService jwtService, RefreshTokenService refreshTokenService, GoogleOAuthService googleOAuthService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.googleOAuthService = googleOAuthService;
    }

//    // ✅ Register via JSON
//    @PostMapping("/register")
//    public String register(@RequestBody RegisterRequest request) {
//        UserAccount user = authService.register(request.getUsername(), request.getEmail(),
//                request.getPassword(), request.getRole());
//        return "Registered: " + user.getUsername();
//    }

//    // ✅ Login returns JWT token
//    @PostMapping("/login")
//    public String login(@RequestBody LoginRequest request) {
//        UserInformation u = authService.loginWithPassword(request.getUsername(), request.getPassword());
//        if (u != null) {
//            String token = jwtService.generateToken(u.getUsername(), u.getRole().toString());
//            return token;
//        } else {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
//        }
//    }
@GetMapping("/google/callback")
public Map<String, String> googleCallback(@RequestParam String code) throws Exception {
    JsonNode userInfo = googleOAuthService.getUserInfo(code);
    String email = userInfo.get("email").asText();
    String googleId = userInfo.get("sub").asText();

    // Try to login or register if not exists
    UserAccount user = authService.loginWithGoogle(googleId);
    if (user == null) {
        user = authService.register("google_" + googleId, email, "N/A", UserRole.CLIENT);
        user.setGoogleId(googleId);
        authService.save(user);
    }

    // Issue tokens
    String accessToken = jwtService.generateToken(user.getUsername(), user.getRole().toString(), user.getId());
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

    return Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken.getToken()
    );
}

@GetMapping("/google/callbackFromGoogle")
public ResponseEntity<?> callbackFromGoogle(@RequestParam String code) {
    try {
        // 1. Lấy token response (access_token, refresh_token, id_token...)
        JsonNode tokenResponse = googleOAuthService.getTokenResponse(code);

        String accessToken = tokenResponse.has("access_token") ? tokenResponse.get("access_token").asText() : null;
        String refreshToken = tokenResponse.has("refresh_token") ? tokenResponse.get("refresh_token").asText() : null;
        String idToken = tokenResponse.has("id_token") ? tokenResponse.get("id_token").asText() : null;

        // 2. (Tuỳ chọn) Lấy thông tin user từ access token
        JsonNode userInfo = null;
        if (accessToken != null) {
            userInfo = googleOAuthService.getUserInfo(accessToken);
        }

        // 3. Trả về dữ liệu cho client
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", accessToken);
        result.put("refresh_token", refreshToken);
        result.put("id_token", idToken);
        result.put("user_info", userInfo);

        return ResponseEntity.ok(result);

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get tokens or user info", "details", e.getMessage()));
    }
}


    @PostMapping("/login-google")
    public String loginGoogle(@RequestBody GoogleLoginRequest request) {
        UserAccount u = authService.loginWithGoogle(request.getGoogleId());
        return (u != null) ? "Logged in with Google" : "Google ID not found";
    }

    @PostMapping("/update")
    public String updateAccount() {
        return "Account updated";
    }

    @PostMapping("/logout")
    public String logout(@RequestBody TokenRefreshRequest request) {
        refreshTokenService.deleteByToken(request.getRefreshToken());
        return "Refresh token deleted successfully. Logged out.";
    }


    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        UserAccount user = authService.loginWithPassword(request.getUsername(), request.getPassword());
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");

        String accessToken = jwtService.generateToken(user.getUsername(), user.getRole().toString(), user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken.getToken()
        );
    }

        @PostMapping("/refresh")
    public TokenRefreshResponse refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService
                .findByToken(requestRefreshToken)
                .flatMap(refreshTokenService::verifyExpiration)
                .map(refreshToken -> {
                    UserAccount user = refreshToken.getUser();
                    String token = jwtService.generateToken(user.getUsername(), user.getRole().toString(), user.getId());
                    return new TokenRefreshResponse(token, requestRefreshToken);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token not found or expired"));
    }



    @GetMapping("/login-google-url")
    public String getGoogleLoginUrl() {
        return googleOAuthService.getGoogleLoginUrl();
    }




}
