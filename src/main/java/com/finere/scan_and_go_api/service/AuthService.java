package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Organization;
import com.finere.scan_and_go_api.domain.entity.User;
import com.finere.scan_and_go_api.domain.enums.OrgType;
import com.finere.scan_and_go_api.domain.enums.UserRole;
import com.finere.scan_and_go_api.dto.auth.AuthResponse;
import com.finere.scan_and_go_api.dto.auth.LoginRequest;
import com.finere.scan_and_go_api.dto.auth.OrganizationApplicationRequest;
import com.finere.scan_and_go_api.dto.auth.RefreshRequest;
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

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    private static final Map<OrgType, UserRole> ADMIN_ROLE_BY_ORG_TYPE = Map.of(
            OrgType.IMPORTER, UserRole.ROLE_IMPORTER_ADMIN,
            OrgType.WHOLESALER, UserRole.ROLE_WHOLESALER_ADMIN,
            OrgType.RETAILER, UserRole.ROLE_RETAILER_ADMIN
    );

    /** Public self-service signup: creates the organization (still PENDING_KYC - a super
     * admin must activate it) and its first admin user together, then logs that admin in
     * immediately so the business can start setting up its catalog/depots while KYC is reviewed. */
    @Transactional
    public AuthResponse applyOrganization(OrganizationApplicationRequest request) {
        UserRole adminRole = ADMIN_ROLE_BY_ORG_TYPE.get(request.orgType());
        if (adminRole == null) {
            throw new IllegalArgumentException("Organizations of type " + request.orgType() + " cannot self-register");
        }
        if (organizationRepository.findByTaxId(request.taxId()).isPresent()) {
            throw new IllegalArgumentException("An organization already exists with this tax ID");
        }
        if (userRepository.findByPhone(request.adminPhone()).isPresent()) {
            throw new BadCredentialsException("A user already exists with this phone number");
        }

        Organization organization = new Organization();
        organization.setName(request.orgName());
        organization.setTaxId(request.taxId());
        organization.setRccm(request.rccm());
        organization.setOrgType(request.orgType());
        organization.setPhone(request.orgPhone());
        organization.setEmail(request.orgEmail());
        organization.setAddress(request.orgAddress());
        organizationRepository.save(organization);

        User admin = new User();
        admin.setOrganization(organization);
        admin.setPhone(request.adminPhone());
        admin.setEmail(request.adminEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        admin.setFirstName(request.adminFirstName());
        admin.setLastName(request.adminLastName());
        admin.setRole(adminRole);
        userRepository.save(admin);

        return issueTokens(new CustomUserDetails(admin));
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
