package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.TripRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TripRecordRepository extends JpaRepository<TripRecord, UUID> {
    Optional<TripRecord> findByTrip(Trip trip);

    boolean existsByTrip(Trip trip);
}
