package Timeout.travel_tackle.trip.dto;

import Timeout.travel_tackle.entity.Enum.TripStatus;
import Timeout.travel_tackle.entity.Trip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TripDetailResponse(
        UUID id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status,
        boolean published,
        LocalDateTime createdAt,
        List<TripDayResponse> days
) {
    public static TripDetailResponse of(Trip trip, List<TripDayResponse> days) {
        return new TripDetailResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus(),
                trip.isPublished(),
                trip.getCreatedAt(),
                days
        );
    }
}
