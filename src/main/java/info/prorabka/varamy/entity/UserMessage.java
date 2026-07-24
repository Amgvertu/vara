package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_messages", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_user_read", columnList = "user_id, is_read")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_message_id", nullable = false)
    private AdminMessage adminMessage;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}