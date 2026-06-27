package Timeout.travel_tackle.trip.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SaveTripRequest(
        @NotNull UUID tripId
) {}
