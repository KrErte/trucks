package eu.fuelfleet.auth.controller;

import eu.fuelfleet.auth.dto.*;
import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.auth.entity.UserInvitation;
import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.auth.service.AuditService;
import eu.fuelfleet.auth.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final AuditService auditService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserListResponse> listUsers(@AuthenticationPrincipal UserPrincipal principal) {
        return userManagementService.listUsers(principal.getCompanyId())
                .stream().map(UserListResponse::fromEntity).toList();
    }

    @PostMapping("/users/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InviteResponse> invite(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InviteRequest request) {
        UserInvitation invitation = userManagementService.invite(
                principal.getCompanyId(), principal.getId(), request.email(), request.role());
        auditService.log(principal.getCompanyId(), principal.getId(),
                "INVITE_USER", "UserInvitation", invitation.getId().toString(),
                "{\"email\":\"" + request.email() + "\",\"role\":\"" + request.role() + "\"}", null);
        return ResponseEntity.status(HttpStatus.CREATED).body(InviteResponse.fromEntity(invitation));
    }

    @PostMapping("/users/invite/accept")
    public ResponseEntity<Map<String, String>> acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        User user = userManagementService.acceptInvitation(
                request.token(), request.firstName(), request.lastName(), request.password());
        return ResponseEntity.ok(Map.of("message", "Account created successfully", "email", user.getEmail()));
    }

    @PutMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserListResponse deactivateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        User user = userManagementService.deactivateUser(id, principal.getCompanyId());
        auditService.log(principal.getCompanyId(), principal.getId(),
                "DEACTIVATE_USER", "User", id.toString(), null, null);
        return UserListResponse.fromEntity(user);
    }

    @PutMapping("/users/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserListResponse reactivateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        User user = userManagementService.reactivateUser(id, principal.getCompanyId());
        auditService.log(principal.getCompanyId(), principal.getId(),
                "REACTIVATE_USER", "User", id.toString(), null, null);
        return UserListResponse.fromEntity(user);
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserListResponse changeRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        User user = userManagementService.changeRole(id, principal.getCompanyId(), body.get("role"));
        auditService.log(principal.getCompanyId(), principal.getId(),
                "CHANGE_ROLE", "User", id.toString(),
                "{\"newRole\":\"" + body.get("role") + "\"}", null);
        return UserListResponse.fromEntity(user);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogResponse> getAuditLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditService.getAuditLogs(principal.getCompanyId(), PageRequest.of(page, size))
                .map(AuditLogResponse::fromEntity);
    }
}
