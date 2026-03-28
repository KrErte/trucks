package eu.fuelfleet.notification.repository;

import eu.fuelfleet.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    List<Notification> findByCompanyIdAndReadFalseOrderByCreatedAtDesc(UUID companyId);
    long countByCompanyIdAndReadFalse(UUID companyId);
}
