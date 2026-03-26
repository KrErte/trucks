package eu.fuelfleet.auth.service;

import eu.fuelfleet.auth.dto.AuthResponse;
import eu.fuelfleet.auth.dto.ChangePasswordRequest;
import eu.fuelfleet.auth.dto.LoginRequest;
import eu.fuelfleet.auth.dto.ProfileResponse;
import eu.fuelfleet.auth.dto.RegisterRequest;
import eu.fuelfleet.auth.dto.UpdateProfileRequest;
import eu.fuelfleet.auth.entity.RefreshToken;
import eu.fuelfleet.auth.entity.Role;
import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.auth.repository.RefreshTokenRepository;
import eu.fuelfleet.auth.repository.UserRepository;
import eu.fuelfleet.auth.security.JwtTokenProvider;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.company.repository.CompanyRepository;
import eu.fuelfleet.subscription.entity.Plan;
import eu.fuelfleet.subscription.entity.Subscription;
import eu.fuelfleet.subscription.entity.SubscriptionStatus;
import eu.fuelfleet.subscription.repository.SubscriptionRepository;
import eu.fuelfleet.vehicle.entity.FuelType;
import eu.fuelfleet.vehicle.entity.Vehicle;
import eu.fuelfleet.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final VehicleRepository vehicleRepository;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        Company company = Company.builder()
                .name(request.companyName())
                .vatNumber(request.vatNumber())
                .country(request.country())
                .regCode(request.regCode())
                .build();
        company = companyRepository.save(company);

        String verificationToken = UUID.randomUUID().toString();
        User user = User.builder()
                .company(company)
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(Role.ADMIN)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(24))
                .build();
        user = userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        Subscription trialSubscription = Subscription.builder()
                .company(company)
                .plan(Plan.ENTERPRISE)
                .status(SubscriptionStatus.TRIALING)
                .currentPeriodEnd(LocalDateTime.now().plusDays(14))
                .build();
        subscriptionRepository.save(trialSubscription);

        createDefaultVehicles(company);

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getRole().name(), company.getId());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getRole().name(), user.getCompany().getId());
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        String newAccessToken = jwtTokenProvider.generateToken(user.getEmail());
        String newRefreshToken = generateRefreshToken(user);

        return new AuthResponse(newAccessToken, newRefreshToken, user.getEmail(), user.getRole().name(), user.getCompany().getId());
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));
        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification token expired");
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setVerificationToken(resetToken);
            user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(email, resetToken);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));
        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token expired");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new ProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        userRepository.save(user);
        return new ProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private void createDefaultVehicles(Company company) {
        List<Vehicle> defaults = List.of(
                Vehicle.builder().company(company).name("Volvo FH 500").fuelType(FuelType.DIESEL)
                        .consumptionLoaded(new BigDecimal("32.5")).consumptionEmpty(new BigDecimal("24.0"))
                        .tankCapacity(new BigDecimal("600")).euroClass("EURO6").active(true).build(),
                Vehicle.builder().company(company).name("Scania R450").fuelType(FuelType.DIESEL)
                        .consumptionLoaded(new BigDecimal("30.0")).consumptionEmpty(new BigDecimal("22.5"))
                        .tankCapacity(new BigDecimal("500")).euroClass("EURO6").active(true).build(),
                Vehicle.builder().company(company).name("MAN TGX 18.510").fuelType(FuelType.DIESEL)
                        .consumptionLoaded(new BigDecimal("33.0")).consumptionEmpty(new BigDecimal("25.0"))
                        .tankCapacity(new BigDecimal("600")).euroClass("EURO6").active(true).build(),
                Vehicle.builder().company(company).name("DAF XF 480").fuelType(FuelType.DIESEL)
                        .consumptionLoaded(new BigDecimal("31.0")).consumptionEmpty(new BigDecimal("23.0"))
                        .tankCapacity(new BigDecimal("550")).euroClass("EURO5").active(true).build(),
                Vehicle.builder().company(company).name("Mercedes Actros 1845").fuelType(FuelType.DIESEL)
                        .consumptionLoaded(new BigDecimal("29.5")).consumptionEmpty(new BigDecimal("22.0"))
                        .tankCapacity(new BigDecimal("500")).euroClass("EURO6").active(true).build()
        );
        vehicleRepository.saveAll(defaults);
    }

    private String generateRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
