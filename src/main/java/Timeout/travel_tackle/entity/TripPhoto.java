package Timeout.travel_tackle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trip_photos")
@Getter
@Setter
public class TripPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(name = "image_url")
    private String imageUrl;

    private String caption;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
