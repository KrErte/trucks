package eu.fuelfleet.notification.service;

import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.auth.repository.UserRepository;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.notification.entity.Notification;
import eu.fuelfleet.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public List<Notification> getAll() {
        return notificationRepository.findByCompanyIdOrderByCreatedAtDesc(getCompanyId());
    }

    public List<Notification> getUnread() {
        return notificationRepository.findByCompanyIdAndReadFalseOrderByCreatedAtDesc(getCompanyId());
    }

    public long getUnreadCount() {
        return notificationRepository.countByCompanyIdAndReadFalse(getCompanyId());
    }

    @Transactional
    public void markAsRead(UUID id) {
        notificationRepository.findById(id).ifPresent(n -> { n.setRead(true); notificationRepository.save(n); });
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.findByCompanyIdAndReadFalseOrderByCreatedAtDesc(getCompanyId())
                .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
    }

    @Transactional
    public Notification create(Company company, String type, String title, String message, String entityType, UUID entityId) {
        Notification n = Notification.builder()
                .company(company).type(type).title(title).message(message)
                .entityType(entityType).entityId(entityId)
                .read(false).emailSent(false)
                .build();
        return notificationRepository.save(n);
    }

    private UUID getCompanyId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow().getCompany().getId();
    }
}
