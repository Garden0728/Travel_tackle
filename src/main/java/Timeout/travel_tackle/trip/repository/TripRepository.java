package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findAllByUserOrderByCreatedAtDesc(User user);

    @Query(value = "select t from Trip t join fetch t.user where t.published = true",
            countQuery = "select count(t) from Trip t where t.published = true")
    Page<Trip> findPublishedWithUser(Pageable pageable);

    @Query("select t from Trip t join fetch t.user where t.id = :id and t.published = true")
    Optional<Trip> findPublishedDetailById(@Param("id") UUID id);
}
