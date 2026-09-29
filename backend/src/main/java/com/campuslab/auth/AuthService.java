package com.campuslab.auth;

import com.campuslab.auth.dto.LoginRequest;
import com.campuslab.auth.dto.LoginResponse;
import com.campuslab.auth.dto.UserSummary;
import com.campuslab.shared.exception.exceptions.InvalidCredentialsException;
import com.campuslab.shared.security.JwtService;
import com.campuslab.user.User;
import com.campuslab.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for user authentication.
 * Handles login, password encoding, and JWT token generation.
 *
 * Requirements: 1.1, 1.2, 1.4
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expiresIn;

    public AuthService(
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${jwt.expiresIn}") long expiresIn
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expiresIn = expiresIn;
    }

    /**
     * Authenticates user credentials and returns a JWT token.
     * Throws InvalidCredentialsException (HTTP 401) without specifying which field is wrong.
     *
     * @param request login credentials
     * @return LoginResponse with token and user summary
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);
        UserSummary userSummary = new UserSummary(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return new LoginResponse(token, "Bearer", expiresIn, userSummary);
    }

    /**
     * Encodes a plain text password using BCrypt.
     * Exposed for use in data seeding or user creation.
     *
     * @param rawPassword the plain text password
     * @return BCrypt hash
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
