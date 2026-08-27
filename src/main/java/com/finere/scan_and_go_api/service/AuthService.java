package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.domain.entity.User;
import com.finere.scan_and_go_api.dto.auth.AuthResponse;
import com.finere.scan_and_go_api.dto.auth.LoginRequest;
import com.finere.scan_and_go_api.dto.auth.RefreshRequest;
import com.finere.scan_and_go_api.dto.auth.RegisterRequest;
import com.finere.scan_and_go_api.repository.OrganizationRepository;
import com.finere.scan_and_go_api.repository.UserRepository;
import com.finere.scan_and_go_api.security.CustomUserDetails;
import com.finere.scan_and_go_api.security.JwtProperties;
import com.finere.scan_and_go_api.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByPhone(request.phone()).isPresent()) {
            throw new BadCredentialsException("A user already exists with this phone number");
        }

        User user = new User();
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role());

        if (request.orgId() != null) {
            Organization org = organizationRepository.findById(request.orgId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown organization: " + request.orgId()));
            user.setOrganization(org);
        }

        userRepository.save(user);
        return issueTokens(new CustomUserDetails(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.phone(), request.password()));

        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        return issueTokens(new CustomUserDetails(user));
    }

    public AuthResponse refresh(RefreshRequest request) {
        Claims claims = jwtService.parseClaims(request.refreshToken());
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadCredentialsException("Not a refresh token");
        }

        User user = userRepository.findById(jwtService.extractUserId(claims))
                .orElseThrow(() -> new BadCredentialsException("Unknown user"));

        return issueTokens(new CustomUserDetails(user));
    }

    private AuthResponse issueTokens(CustomUserDetails userDetails) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        return AuthResponse.of(accessToken, refreshToken, jwtProperties.accessTokenTtlMinutes() * 60);
    }
}
