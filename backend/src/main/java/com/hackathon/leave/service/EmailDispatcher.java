package com.hackathon.leave.service;

import com.hackathon.leave.config.LeaveProperties;
import com.hackathon.leave.model.EmailOutbox;
import com.hackathon.leave.repository.EmailOutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delivers queued notification emails over SMTP. Emails are always written to the outbox; they are only sent when
 * MAIL_HOST is configured. A failing message is retried on later runs and abandoned after 5 attempts.
 */
@Slf4j
@Component
public class EmailDispatcher {

    private static final int MAX_ATTEMPTS = 5;

    private final EmailOutboxRepository outbox;
    private final ObjectProvider<JavaMailSender> sender;
    private final LeaveProperties props;
    private final String host;

    public EmailDispatcher(EmailOutboxRepository outbox, ObjectProvider<JavaMailSender> sender, LeaveProperties props,
                           @Value("${spring.mail.host:}") String host) {
        this.outbox = outbox;
        this.sender = sender;
        this.props = props;
        this.host = host;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    public void dispatch() {
        JavaMailSender mail = sender.getIfAvailable();
        if (mail == null || host == null || host.isBlank()) {
            return;
        }
        for (EmailOutbox e : outbox.findBySentFalseAndAttemptsLessThanOrderByIdAsc(MAX_ATTEMPTS)) {
            try {
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(props.mail().from());
                m.setTo(e.getToEmail());
                m.setSubject(e.getSubject());
                m.setText(e.getBody());
                mail.send(m);
                e.setSent(true);
            } catch (RuntimeException ex) {
                e.setAttempts(e.getAttempts() + 1);
                log.warn("Could not email {} (attempt {}): {}", e.getToEmail(), e.getAttempts(), ex.getMessage());
            }
        }
    }
}
