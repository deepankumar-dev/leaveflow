package com.hackathon.leave.service;

import com.hackathon.leave.dto.NotificationDto;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.Notification;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.NotificationRepository;
import com.hackathon.leave.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationQueryService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationQueryService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        return notifications.findByUserOrderByCreatedAtDesc(u).stream().map(NotificationQueryService::toDto).toList();
    }

    public NotificationDto markRead(Long userId, Long id) {
        Notification n = notifications.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("That notification is not yours");
        }
        n.setRead(true);
        return toDto(n);
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt(),
                n.getRelatedRequest() == null ? null : n.getRelatedRequest().getId());
    }
}
