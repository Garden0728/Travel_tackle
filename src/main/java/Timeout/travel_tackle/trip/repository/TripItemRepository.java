package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.TripDay;
import Timeout.travel_tackle.entity.TripItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripItemRepository extends JpaRepository<TripItem, UUID> {
    List<TripItem> findAllByTripDayOrderByOrderIndex(TripDay tripDay);
    void deleteAllByTripDay(TripDay tripDay);
}
