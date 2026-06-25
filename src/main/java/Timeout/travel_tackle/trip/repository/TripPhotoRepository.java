package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.TripPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TripPhotoRepository extends JpaRepository<TripPhoto, UUID> {
    List<TripPhoto> findAllByTripOrderByUploadedAtDesc(Trip trip);

    List<TripPhoto> findAllByTripInOrderByUploadedAtAsc(Collection<Trip> trips);

    void deleteAllByTrip(Trip trip);
}
