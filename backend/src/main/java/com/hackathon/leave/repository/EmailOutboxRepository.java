package com.hackathon.leave.repository;

import com.hackathon.leave.model.EmailOutbox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {
    List<EmailOutbox> findBySentFalse();

    List<EmailOutbox> findBySentFalseAndAttemptsLessThanOrderByIdAsc(int maxAttempts);
}
