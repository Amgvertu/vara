package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "link", length = 512)
    private String link;

    @Column(name = "category", nullable = false, length = 20)
    private String category; // "INTERNAL" или "PUSH"

    @Column(name = "delivery_type", nullable = false, length = 20)   // <-- новое поле
    private String deliveryType;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_criteria", columnDefinition = "jsonb")
    private DeliveryCriteria deliveryCriteria;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}