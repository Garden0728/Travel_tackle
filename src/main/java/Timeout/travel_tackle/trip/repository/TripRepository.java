package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findAllByUserOrderByCreatedAtDesc(User user);

    @Query("select t from Trip t join fetch t.user where t.published = true and t.id in :ids")
    List<Trip> findPublishedByIdInWithUser(@Param("ids") Collection<UUID> ids);

    @Query(value = "select t from Trip t join fetch t.user where t.published = true",
            countQuery = "select count(t) from Trip t where t.published = true")
    Page<Trip> findPublishedWithUser(Pageable pageable);

    // 참견(TripFeedback) 수 내림차순, 동점은 최신순 — 정렬이 쿼리에 고정되므로 pageable 은 unsorted 로 넘긴다
    @Query(value = "select t from Trip t join fetch t.user where t.published = true "
            + "order by (select count(f) from TripFeedback f where f.trip = t) desc, t.createdAt desc",
            countQuery = "select count(t) from Trip t where t.published = true")
    Page<Trip> findPublishedWithUserOrderByFeedbackCount(Pageable pageable);

    @Query("select t from Trip t join fetch t.user where t.id = :id and t.published = true")
    Optional<Trip> findPublishedDetailById(@Param("id") UUID id);
}
