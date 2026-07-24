package info.prorabka.varamy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_notification_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserNotificationSubscription {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private SubscriptionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionId implements java.io.Serializable {
        @Column(name = "user_id", columnDefinition = "uuid")
        private UUID userId;

        @Column(name = "type")
        private Integer type;

        @Column(name = "sub_type")
        private Integer subType;
    }
}