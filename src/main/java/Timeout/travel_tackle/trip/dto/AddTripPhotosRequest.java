package Timeout.travel_tackle.trip.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddTripPhotosRequest(
        @NotEmpty @Valid List<PhotoEntry> photos
) {
    public record PhotoEntry(
            @NotBlank String imageUrl,
            @Size(max = 500) String caption
    ) {}
}
