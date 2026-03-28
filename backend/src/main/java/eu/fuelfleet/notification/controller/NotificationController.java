package eu.fuelfleet.notification.controller;

import eu.fuelfleet.notification.entity.Notification;
import eu.fuelfleet.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> getAll() { return notificationService.getAll(); }

    @GetMapping("/unread")
    public List<Notification> getUnread() { return notificationService.getUnread(); }

    @GetMapping("/unread/count")
    public Map<String, Long> getUnreadCount() { return Map.of("count", notificationService.getUnreadCount()); }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) { notificationService.markAsRead(id); return ResponseEntity.ok().build(); }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() { notificationService.markAllAsRead(); return ResponseEntity.ok().build(); }
}
