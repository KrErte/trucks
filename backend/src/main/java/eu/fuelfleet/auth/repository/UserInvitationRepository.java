package eu.fuelfleet.auth.repository;

import eu.fuelfleet.auth.entity.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {
    Optional<UserInvitation> findByToken(String token);
    Optional<UserInvitation> findByEmailAndCompanyIdAndAcceptedAtIsNull(String email, UUID companyId);
}
