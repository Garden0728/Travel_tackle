package Timeout.travel_tackle.trip.dto;

import jakarta.validation.constraints.Size;

public record UpdateTripPhotoRequest(
        @Size(max = 500) String caption
) {}
