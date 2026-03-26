package eu.fuelfleet.auth.service;

import eu.fuelfleet.auth.entity.Role;
import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.auth.entity.UserInvitation;
import eu.fuelfleet.auth.repository.UserInvitationRepository;
import eu.fuelfleet.auth.repository.UserRepository;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserInvitationRepository invitationRepository;
    private final CompanyService companyService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> listUsers(UUID companyId) {
        return userRepository.findByCompanyId(companyId);
    }

    @Transactional
    public UserInvitation invite(UUID companyId, UUID invitedByUserId, String email, String roleName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
        }

        invitationRepository.findByEmailAndCompanyIdAndAcceptedAtIsNull(email, companyId)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already sent to this email");
                });

        Company company = companyService.getCompany(companyId);
        User inviter = userRepository.findById(invitedByUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inviter not found"));

        Role role;
        try {
            role = Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName);
        }

        String token = UUID.randomUUID().toString();

        UserInvitation invitation = UserInvitation.builder()
                .company(company)
                .email(email)
                .role(role)
                .token(token)
                .invitedByUser(inviter)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        invitation = invitationRepository.save(invitation);

        emailService.sendInviteEmail(email, token, company.getName());

        return invitation;
    }

    @Transactional
    public User acceptInvitation(String token, String firstName, String lastName, String password) {
        UserInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (invitation.isAccepted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation already accepted");
        }
        if (invitation.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation has expired");
        }

        User user = User.builder()
                .company(invitation.getCompany())
                .email(invitation.getEmail())
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .role(invitation.getRole())
                .emailVerified(true)
                .active(true)
                .invitedBy(invitation.getInvitedByUser().getId())
                .build();

        user = userRepository.save(user);

        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        return user;
    }

    @Transactional
    public User deactivateUser(UUID userId, UUID companyId) {
        User user = getCompanyUser(userId, companyId);
        user.setActive(false);
        user.setDeactivatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public User reactivateUser(UUID userId, UUID companyId) {
        User user = getCompanyUser(userId, companyId);
        user.setActive(true);
        user.setDeactivatedAt(null);
        return userRepository.save(user);
    }

    @Transactional
    public User changeRole(UUID userId, UUID companyId, String roleName) {
        User user = getCompanyUser(userId, companyId);
        Role role;
        try {
            role = Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName);
        }
        user.setRole(role);
        return userRepository.save(user);
    }

    private User getCompanyUser(UUID userId, UUID companyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not belong to your company");
        }
        return user;
    }
}
