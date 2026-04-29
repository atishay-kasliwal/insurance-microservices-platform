package com.microservices.demo.gateway.service.controller;

import com.microservices.demo.gateway.service.model.AuthRequest;
import com.microservices.demo.gateway.service.model.AuthResponse;
import com.microservices.demo.gateway.service.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${gateway.auth.username:admin}")
    private String adminUsername;

    @Value("${gateway.auth.password:admin}")
    private String adminPassword;

    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        if (adminUsername.equals(request.getUsername())
                && adminPassword.equals(request.getPassword())) {
            String token = jwtTokenProvider.generateToken(request.getUsername(), "USER");
            log.info("JWT issued for user '{}'", request.getUsername());
            return Mono.just(ResponseEntity.ok(
                new AuthResponse(token, jwtTokenProvider.getExpirationMs())
            ));
        }
        log.warn("Failed login attempt for user '{}'", request.getUsername());
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/validate")
    public Mono<ResponseEntity<String>> validate(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                return Mono.just(ResponseEntity.ok("Valid token for user: " + username));
            }
        }
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token"));
    }
}
