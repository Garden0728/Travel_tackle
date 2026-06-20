package Timeout.travel_tackle.entity;

import Timeout.travel_tackle.entity.Enum.TripStatus;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_published")
    private boolean published; //계획 공개 여부 다른 사용자가 볼 수 있게 pullic이냐 private냐

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status = TripStatus.PLANNING; //여행 진행 상태 계획중 or 여행 완료

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; //계획 생성 날짜

    public Trip(User user, String title, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new CustomException(ErrorCode.INVALID_TRIP_DATE_RANGE);
        }
        this.user = user;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateSchedule(String title, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new CustomException(ErrorCode.INVALID_TRIP_DATE_RANGE); //종료일이 시작일 보단 빠르면 예외 발생
        }
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void publish() {
        this.published = true;
    }

    public void unpublish() {
        this.published = false;
    }

    public void complete() {
        this.status = TripStatus.COMPLETED;
    }
}
