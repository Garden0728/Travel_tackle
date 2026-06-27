package Timeout.travel_tackle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trip_records", uniqueConstraints = @UniqueConstraint( // 계획당 기록 1개 강제
        columnNames = "trip_id"
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripRecord { // 계획 기반 여행 기록(후기). 내용 필수, 사진 1장 이상(TripPhoto)

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, length = 2000)
    private String content; // 후기 내용(필수)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TripRecord(Trip trip, String content) {
        this.trip = trip;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
