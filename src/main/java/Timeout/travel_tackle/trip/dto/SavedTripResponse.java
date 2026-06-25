package Timeout.travel_tackle.trip.dto;

import Timeout.travel_tackle.entity.SavedTrip;

import java.time.LocalDateTime;
import java.util.UUID;

public record SavedTripResponse(
        UUID savedTripId,
        UUID originalTripId,
        String originalTitle,
        String ownerName,
        LocalDateTime savedAt
) {
    public static SavedTripResponse from(SavedTrip savedTrip) {
        return new SavedTripResponse(
                savedTrip.getId(),
                savedTrip.getOriginalTrip().getId(),
                savedTrip.getOriginalTrip().getTitle(),
                savedTrip.getOriginalTrip().getUser().getName(),
                savedTrip.getSavedAt()
        );
    }
}
