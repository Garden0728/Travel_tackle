package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.TripFeedbackRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripFeedbackRecommendationRepository extends JpaRepository<TripFeedbackRecommendation, UUID> {

    List<TripFeedbackRecommendation> findAllByFeedbackId(UUID feedbackId);

    void deleteAllByFeedbackId(UUID feedbackId);

    void deleteAllByFeedbackIdIn(List<UUID> feedbackIds);
}
