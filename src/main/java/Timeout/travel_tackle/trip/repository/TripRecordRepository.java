package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.TripRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRecordRepository extends JpaRepository<TripRecord, UUID> {
    Optional<TripRecord> findByTrip(Trip trip);

    boolean existsByTrip(Trip trip);

    /**
     * 추천 탭(기록 추천)용 — 공개된 여러 계획의 기록을 작성자/계획과 함께 한 번에 조회.
     */
    @Query("select r from TripRecord r join fetch r.trip t join fetch t.user "
            + "where t.published = true and t.id in :tripIds")
    List<TripRecord> findPublishedByTripIdInWithTripAndUser(@Param("tripIds") Collection<UUID> tripIds);
}
