package com.llms.service;

import com.llms.dto.request.LoginRequest;
import com.llms.dto.request.RegisterRequest;
import com.llms.dto.response.AuthResponse;
import com.llms.entity.User;
import com.llms.exception.AuthenticationException;
import com.llms.exception.DuplicateResourceException;
import com.llms.repository.UserRepository;
import com.llms.security.JwtTokenProvider;
import com.llms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        user = userRepository.save(user);
        log.info("User registered: {} with role {}", user.getEmail(), user.getRole());

        UserPrincipal principal = new UserPrincipal(user);
        String token = tokenProvider.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole())
                .name(user.getName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String token = tokenProvider.generateToken(principal);

            log.info("User logged in: {}", request.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .role(principal.getRole())
                    .name(principal.getName())
                    .build();
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }
    }
}
