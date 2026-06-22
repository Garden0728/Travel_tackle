package Timeout.travel_tackle.entity;

import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "trip_items", uniqueConstraints = @UniqueConstraint(
        columnNames = {"trip_day_id", "order_index"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_day_id", nullable = false)
    private TripDay tripDay; //외래키 주인 TripDay랑 다대일 관계를 가져서 해당 일에 어디 방문할지 담아둠

    @Column(name = "tour_api_content_id", nullable = false)
    private String tourApiContentId; //투어 api 헤당 관광지 id

    @Column(name = "cached_title", nullable = false)
    private String cachedTitle; //관광지 이름

    @Column(name = "cached_image_url")
    private String cachedImageUrl; //사진 url

    @Column(name = "start_time")
    private LocalTime startTime; //방문 시간

    @Column(name = "end_time") //나오는 시간
    private LocalTime endTime;

    @Column(name = "order_index")
    private int orderIndex; //관고아 방문 순서

    public TripItem(TripDay tripDay, String tourApiContentId, String cachedTitle,
                    String cachedImageUrl, LocalTime startTime, LocalTime endTime,
                    int orderIndex) {
        validateTime(startTime, endTime);
        validateOrderIndex(orderIndex);
        this.tripDay = tripDay;
        this.tourApiContentId = tourApiContentId;
        this.cachedTitle = cachedTitle;
        this.cachedImageUrl = cachedImageUrl;
        this.startTime = startTime;
        this.endTime = endTime;
        this.orderIndex = orderIndex;
    }

    public void changeTime(LocalTime startTime, LocalTime endTime) {
        validateTime(startTime, endTime);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void changeOrder(int orderIndex) {
        validateOrderIndex(orderIndex);
        this.orderIndex = orderIndex;
    }

    public void moveTo(TripDay newTripDay, int newOrderIndex) {
        validateOrderIndex(newOrderIndex);
        this.tripDay = newTripDay;
        this.orderIndex = newOrderIndex;
    }

    private static void validateTime(LocalTime startTime, LocalTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new CustomException(ErrorCode.INVALID_TRIP_ITEM_TIME_RANGE);
        }
    }

    private static void validateOrderIndex(int orderIndex) {
        if (orderIndex < 0) {
            throw new CustomException(ErrorCode.INVALID_ORDER_INDEX);
        }
    }
}
