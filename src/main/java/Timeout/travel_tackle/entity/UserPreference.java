package Timeout.travel_tackle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "travel_style")
    private String travelStyle;

    @Column(name = "budget_level")
    private String budgetLevel;

    @Column(name = "preferred_region")
    private String preferredRegion;

    @Column(name = "interest_tags")
    private String interestTags;
}
