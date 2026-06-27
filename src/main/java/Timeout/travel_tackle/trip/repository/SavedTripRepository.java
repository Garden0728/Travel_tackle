package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.SavedTrip;
import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedTripRepository extends JpaRepository<SavedTrip, UUID> {
    boolean existsByUserAndOriginalTrip(User user, Trip originalTrip);

    Optional<SavedTrip> findByIdAndUser(UUID id, User user);

    void deleteAllByOriginalTrip(Trip originalTrip);

    @Query("select s from SavedTrip s join fetch s.originalTrip t join fetch t.user "
            + "where s.user = :user order by s.savedAt desc")
    List<SavedTrip> findAllWithOriginalByUser(@Param("user") User user);
}
