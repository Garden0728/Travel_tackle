package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findAllByUserOrderByCreatedAtDesc(User user);
}
