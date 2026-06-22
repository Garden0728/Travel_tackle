package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.CartItem;
import Timeout.travel_tackle.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findAllByUserOrderByAddedAtDesc(User user);
    boolean existsByUserAndTourApiContentId(User user, String tourApiContentId);
    void deleteByUserAndTourApiContentId(User user, String tourApiContentId);
}
