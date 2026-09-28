package com.hackathon.leave.service;

import com.hackathon.leave.model.EmailOutbox;
import com.hackathon.leave.model.LeaveRequest;
import com.hackathon.leave.model.Notification;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.EmailOutboxRepository;
import com.hackathon.leave.repository.NotificationRepository;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** In-app notification plus a simulated email (an outbox row that is never actually sent). */
@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final EmailOutboxRepository outbox;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, EmailOutboxRepository outbox, Clock clock) {
        this.notifications = notifications;
        this.outbox = outbox;
        this.clock = clock;
    }

    public void notify(User recipient, String message, LeaveRequest related) {
        if (recipient == null) {
            return;
        }
        Notification n = new Notification();
        n.setUser(recipient);
        n.setMessage(message);
        n.setCreatedAt(clock.instant());
        n.setRelatedRequest(related);
        notifications.save(n);

        EmailOutbox mail = new EmailOutbox();
        mail.setToEmail(recipient.getEmail());
        mail.setSubject("Leave update");
        mail.setBody(message);
        mail.setCreatedAt(clock.instant());
        outbox.save(mail);
        log.info("EMAIL (simulated) to {}: {}", recipient.getEmail(), message);
    }
}
