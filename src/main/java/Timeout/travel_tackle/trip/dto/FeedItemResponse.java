package Timeout.travel_tackle.trip.dto;

import Timeout.travel_tackle.entity.Enum.TripStatus;
import Timeout.travel_tackle.entity.Trip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record FeedItemResponse(
        UUID tripId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status,
        String ownerName,
        String thumbnailUrl,
        long feedbackCount,
        LocalDateTime createdAt
) {
    public static FeedItemResponse of(Trip trip, String thumbnailUrl, long feedbackCount) {
        return new FeedItemResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus(),
                trip.getUser().getName(),
                thumbnailUrl,
                feedbackCount,
                trip.getCreatedAt()
        );
    }
}
