package Timeout.travel_tackle.trip.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalTime;

public record AddTripItemRequest(
        @NotBlank String tourApiContentId,
        @NotBlank String cachedTitle,
        String cachedImageUrl,
        LocalTime startTime,
        LocalTime endTime
) {}
