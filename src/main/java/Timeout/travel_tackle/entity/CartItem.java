package Timeout.travel_tackle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "tour_api_content_id"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tour_api_content_id", nullable = false)
    private String tourApiContentId; //Tour API에서 제공하는 관광 컨테츠 고유 ID

    @Column(name = "cached_title", nullable = false)
    private String cachedTitle; //관광지 이름

    @Column(name = "cached_image_url")
    private String cachedImageUrl; //관광지 사진

    @Column(name = "cached_region_code")
    private String cachedRegionCode; //관광지 지역 코드

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt; //바구니에 담은 시간

    public CartItem(User user, String tourApiContentId, String cachedTitle,
                    String cachedImageUrl, String cachedRegionCode) {
        this.user = user;
        this.tourApiContentId = tourApiContentId;
        this.cachedTitle = cachedTitle;
        this.cachedImageUrl = cachedImageUrl;
        this.cachedRegionCode = cachedRegionCode;
    }
}
