package Timeout.travel_tackle.entity;

import Timeout.travel_tackle.entity.Enum.InterestTag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "travel_style")
    private String travelStyle;

    @Column(name = "budget_level")
    private String budgetLevel;

    @Column(name = "preferred_region")
    private String preferredRegion;

    @ElementCollection
    @CollectionTable(
            name = "user_preference_interest_tags",
            joinColumns = @JoinColumn(name = "user_preference_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"user_preference_id", "interest_tag"}
            )
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "interest_tag", nullable = false)
    private Set<InterestTag> interestTags = new HashSet<>();

    public UserPreference(User user, String travelStyle, String budgetLevel,
                          String preferredRegion, Set<InterestTag> interestTags) {
        this.user = user;
        this.travelStyle = travelStyle;
        this.budgetLevel = budgetLevel;
        this.preferredRegion = preferredRegion;
        this.interestTags.addAll(interestTags);
    }

    public Set<InterestTag> getInterestTags() {
        return Collections.unmodifiableSet(interestTags);
    }

    public void update(String travelStyle, String budgetLevel,
                       String preferredRegion, Set<InterestTag> interestTags) {
        this.travelStyle = travelStyle;
        this.budgetLevel = budgetLevel;
        this.preferredRegion = preferredRegion;
        this.interestTags.clear();
        this.interestTags.addAll(interestTags);
    }
}
