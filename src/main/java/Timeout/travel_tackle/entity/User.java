package Timeout.travel_tackle.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(nullable = false)
    private String name;

    private String nationality; //사용자 국적 코드

    @Column(name = "credit_balance")
    private int creditBalance; //크레딧 잔액

    @Column(name = "free_trials_used")
    private int freeTrialsUsed; //크레딧 없이 무료로 다른 사용자의 계획을 저장한 횟수

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public User(String email, String name, String nationality) {
        this.email = email;
        this.name = name;
        this.nationality = nationality;
    }

    public static User localUser(String email, String passwordHash, String name, String nationality) {
        User user = new User(email, name, nationality);
        user.passwordHash = passwordHash;
        user.emailVerifiedAt = LocalDateTime.now();
        return user;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void useFreeTrial() {
        this.freeTrialsUsed++;
    }

    public void changeCreditBalance(int amount) {
        this.creditBalance += amount;
    }
}
