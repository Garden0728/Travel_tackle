package Timeout.travel_tackle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trip_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPhoto { //이미지 외부에 저장후 그 이미지 url관리

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    private String caption; //괸광지 설명

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public TripPhoto(Trip trip, String imageUrl, String caption) {
        this.trip = trip;
        this.imageUrl = imageUrl;
        this.caption = caption;
    }

    public void updateCaption(String caption) {
        this.caption = caption;
    }
}
