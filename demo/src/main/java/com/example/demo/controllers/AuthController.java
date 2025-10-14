package com.example.demo.controllers;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.models.User;
import com.example.demo.services.JwtService;
import com.example.demo.services.UserService;
import com.example.demo.services.CookieService;
import jakarta.servlet.http.HttpServletResponse;
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
        User user = userService.findByEmail(request.getEmail());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        response.setHeader("Set-Cookie", cookieService.createAuthCookie(token));

        return ResponseEntity.ok(new AuthResponse(user));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        if (userService.existsByEmail(request.getEmail())) {
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

        return ResponseEntity.ok(new AuthResponse(user));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.setHeader("Set-Cookie", cookieService.deleteAuthCookie());

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Le principal est l'email (String), pas l'objet User
        String email = (String) authentication.getPrincipal();
        User user = userService.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponseDTO userResponse = new UserResponseDTO(user);

        return ResponseEntity.ok(userResponse);
    }
}