package Timeout.travel_tackle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "trip_items")
@Getter
@Setter
public class TripItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_day_id")
    private TripDay tripDay;

    @Column(name = "tour_api_content_id")
    private String tourApiContentId;

    @Column(name = "cached_title")
    private String cachedTitle;

    @Column(name = "cached_image_url")
    private String cachedImageUrl;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "order_index")
    private int orderIndex;
}
