package Timeout.travel_tackle.trip.dto;

import Timeout.travel_tackle.entity.Enum.TripStatus;
import Timeout.travel_tackle.entity.Trip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PublicTripDetailResponse(
        UUID id,
        String title,
        String region,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status,
        String ownerName,
        LocalDateTime createdAt,
        List<TripDayResponse> days,
        TripRecordResponse record,
        long feedbackCount,
        boolean saved
) {
    public static PublicTripDetailResponse of(
            Trip trip,
            String region,
            List<TripDayResponse> days,
            TripRecordResponse record,
            long feedbackCount,
            boolean saved
    ) {
        return new PublicTripDetailResponse(
                trip.getId(),
                trip.getTitle(),
                region,
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus(),
                trip.getUser().getName(),
                trip.getCreatedAt(),
                days,
                record,
                feedbackCount,
                saved
        );
    }
}
