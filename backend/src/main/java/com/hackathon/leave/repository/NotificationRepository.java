package com.hackathon.leave.repository;

import com.hackathon.leave.model.Notification;
import com.hackathon.leave.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
}
