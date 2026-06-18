package Timeout.travel_tackle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;
    private String name;
    private String nationality;

    @Column(name = "credit_balance")
    private int creditBalance;

    @Column(name = "free_trials_used")
    private int freeTrialsUsed;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
