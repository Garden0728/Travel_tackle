package Timeout.travel_tackle.entity;

import Timeout.travel_tackle.entity.Enum.CreditTransactionReason;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditTransaction { //크레딧 사용내역 혹은 충전 내역 관리

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private int amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTransactionReason reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CreditTransaction(User user, int amount, CreditTransactionReason reason) {
        this.user = user;
        this.amount = amount;
        this.reason = reason;
    }
}
