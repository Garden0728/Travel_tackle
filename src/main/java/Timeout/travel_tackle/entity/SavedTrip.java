package Timeout.travel_tackle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saved_trips", uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "original_trip_id"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_trip_id", nullable = false)
    private Trip originalTrip;

    // 스크랩(찜)만 한 상태에서는 비어 있다가, 사용자가 "내 계획으로 복사하기"를 눌러야 채워진다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copied_trip_id")
    private Trip copiedTrip;

    @CreationTimestamp
    @Column(name = "saved_at", nullable = false, updatable = false)
    private LocalDateTime savedAt;

    public SavedTrip(User user, Trip originalTrip) {
        this.user = user;
        this.originalTrip = originalTrip;
    }

    public void markCopied(Trip copiedTrip) {
        this.copiedTrip = copiedTrip;
    }
}
