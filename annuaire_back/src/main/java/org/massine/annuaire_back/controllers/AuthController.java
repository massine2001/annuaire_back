package org.massine.annuaire_back.controllers;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.models.User;
import com.example.demo.services.JwtService;
import com.example.demo.services.UserService;
import com.example.demo.services.CookieService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final CookieService cookieService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(
            UserService userService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            CookieService cookieService
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.cookieService = cookieService;
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        long startTime = System.currentTimeMillis();
        logger.info("Login attempt for email: {}", request.getEmail());

        long dbStart = System.currentTimeMillis();
        User user = userService.findByEmail(request.getEmail());
        logger.info("DB query took {} ms", System.currentTimeMillis() - dbStart);

        if (user == null) {
            logger.warn("User not found for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        long passwordStart = System.currentTimeMillis();
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        logger.info("Password check took {} ms", System.currentTimeMillis() - passwordStart);

        if (!passwordMatches) {
            logger.warn("Invalid password for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        long tokenStart = System.currentTimeMillis();
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        logger.info("Token generation took {} ms", System.currentTimeMillis() - tokenStart);

        response.setHeader("Set-Cookie", cookieService.createAuthCookie(token));

        logger.info("Login successful for {} in {} ms", user.getEmail(), System.currentTimeMillis() - startTime);

        return ResponseEntity.ok(new AuthResponse(user));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        if (userService.existsByEmail(request.getEmail())) {
            logger.warn("Email already in use: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        user = userService.createUser(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        response.setHeader("Set-Cookie", cookieService.createAuthCookie(token));

        logger.info("User registered successfully: {}", user.getEmail());

        return ResponseEntity.ok(new AuthResponse(user));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.setHeader("Set-Cookie", cookieService.deleteAuthCookie());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String email = (String) authentication.getPrincipal();
            logger.info("User logged out: {}", email);
        }

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = (String) authentication.getPrincipal();
        User user = userService.findByEmailSafe(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponseDTO userResponse = new UserResponseDTO(user);

        return ResponseEntity.ok(userResponse);
    }
}