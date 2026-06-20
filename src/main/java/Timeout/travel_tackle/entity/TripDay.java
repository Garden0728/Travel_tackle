package Timeout.travel_tackle.entity;

import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trip_days", uniqueConstraints = @UniqueConstraint( //trip_days이랑 연결 해소 해당 여행의 일차 별로 계획
        columnNames = {"trip_id", "day_number"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "day_number")
    private int dayNumber;

    @Column(nullable = false)
    private LocalDate date;

    public TripDay(Trip trip, int dayNumber, LocalDate date) {
        if (dayNumber < 1) {
            throw new CustomException(ErrorCode.INVALID_DAY_NUMBER);
        }
        this.trip = trip;
        this.dayNumber = dayNumber;
        this.date = date;
    }
}
