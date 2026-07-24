package info.prorabka.varamy.service;

import info.prorabka.varamy.dto.request.FeedbackRequest;
import info.prorabka.varamy.entity.FeedbackMessage;
import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.repository.FeedbackMessageRepository;
import info.prorabka.varamy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {
    private final FeedbackMessageRepository feedbackRepository;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${feedback.email.to:feedback@katok.pro}")
    private String feedbackEmail;

    @Transactional
    public void sendFeedback(UUID userId, FeedbackRequest request) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        FeedbackMessage msg = new FeedbackMessage();
        msg.setUser(user);
        msg.setFullName(request.getFullName());
        msg.setPhone(request.getPhone());
        msg.setEmail(request.getEmail());
        msg.setSubject(request.getSubject());
        msg.setMessage(request.getMessage());
        msg.setStatus(FeedbackMessage.FeedbackStatus.NEW);
        feedbackRepository.save(msg);

        try {
            sendEmail(request.getFullName(), request.getPhone(), request.getEmail(),
                    request.getSubject(), request.getMessage());
            msg.setStatus(FeedbackMessage.FeedbackStatus.SENT);
            feedbackRepository.save(msg);
            log.info("Feedback sent successfully from {} <{}>", request.getFullName(), request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send feedback email", e);
            msg.setStatus(FeedbackMessage.FeedbackStatus.FAILED);
            feedbackRepository.save(msg);
            throw new RuntimeException("Не удалось отправить сообщение, попробуйте позже", e);
        }

    }

    private void sendEmail(String fullName, String phone, String email, String subject, String message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom("katok@katok.pro");
        mail.setTo(feedbackEmail);
        mail.setSubject("Обратная связь: " + subject);
        String text = String.format("""
                Отправитель: %s
                Телефон: %s
                E-mail: %s
                
                Сообщение:
                %s
                """, fullName, phone, email, message);
        mail.setText(text);
        mailSender.send(mail);
    }
}