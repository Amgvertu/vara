package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_messages")
@Data
@NoArgsConstructor
public class FeedbackMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)   // теперь может быть null
    private User user;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FeedbackStatus status = FeedbackStatus.NEW;

    public enum FeedbackStatus {
        NEW, SENT, FAILED
    }
}
