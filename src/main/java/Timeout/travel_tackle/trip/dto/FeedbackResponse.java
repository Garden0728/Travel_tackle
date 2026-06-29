package Timeout.travel_tackle.trip.dto;

import Timeout.travel_tackle.entity.TripFeedback;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        AuthorInfo author,
        String content,
        UUID tripDayId,
        UUID tripItemId,
        List<FeedbackRecommendationResponse> recommendations,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record AuthorInfo(UUID id, String name) {}

    public static FeedbackResponse of(TripFeedback feedback, List<FeedbackRecommendationResponse> recommendations) {
        return new FeedbackResponse(
                feedback.getId(),
                new AuthorInfo(feedback.getAuthor().getId(), feedback.getAuthor().getName()),
                feedback.getContent(),
                feedback.getTripDay() != null ? feedback.getTripDay().getId() : null,
                feedback.getTripItem() != null ? feedback.getTripItem().getId() : null,
                recommendations,
                feedback.isRead(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }
}
