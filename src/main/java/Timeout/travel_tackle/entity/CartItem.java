package Timeout.travel_tackle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "tour_api_content_id")
    private String tourApiContentId;

    @Column(name = "cached_title")
    private String cachedTitle;

    @Column(name = "cached_image_url")
    private String cachedImageUrl;

    @Column(name = "cached_region_code")
    private String cachedRegionCode;

    @Column(name = "added_at")
    private LocalDateTime addedAt;
}
