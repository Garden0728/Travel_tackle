package Timeout.travel_tackle.trip.dto;

import Timeout.travel_tackle.entity.SavedTrip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SavedTripResponse(
        UUID savedTripId,
        UUID originalTripId,
        String originalTitle,
        String ownerName,
        String region,
        LocalDate startDate,
        LocalDate endDate,
        String thumbnailUrl,
        long feedbackCount,
        long saveCount,
        UUID copiedTripId,
        LocalDateTime savedAt,
        List<TripDayResponse> days
) {
    public static SavedTripResponse of(
            SavedTrip savedTrip, String region, String thumbnailUrl, long feedbackCount, long saveCount,
            List<TripDayResponse> days
    ) {
        return new SavedTripResponse(
                savedTrip.getId(),
                savedTrip.getOriginalTrip().getId(),
                savedTrip.getOriginalTrip().getTitle(),
                savedTrip.getOriginalTrip().getUser().getName(),
                region,
                savedTrip.getOriginalTrip().getStartDate(),
                savedTrip.getOriginalTrip().getEndDate(),
                thumbnailUrl,
                feedbackCount,
                saveCount,
                savedTrip.getCopiedTrip() != null ? savedTrip.getCopiedTrip().getId() : null,
                savedTrip.getSavedAt(),
                days
        );
    }
}
