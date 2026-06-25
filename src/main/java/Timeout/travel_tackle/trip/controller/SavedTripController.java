package Timeout.travel_tackle.trip.controller;

import Timeout.travel_tackle.trip.dto.SaveTripRequest;
import Timeout.travel_tackle.trip.dto.SavedTripResponse;
import Timeout.travel_tackle.trip.dto.TripSummaryResponse;
import Timeout.travel_tackle.trip.service.SavedTripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/saved-trips")
@RequiredArgsConstructor
@Tag(name = "Saved Trip", description = "다른 사용자 계획 저장 API")
public class SavedTripController {

    private final SavedTripService savedTripService;

    @PostMapping
    @Operation(summary = "다른 사용자의 공개 여행을 내 계획으로 저장(복사)")
    public ResponseEntity<TripSummaryResponse> save(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SaveTripRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedTripService.save(userId, request.tripId()));
    }

    @GetMapping
    @Operation(summary = "내가 저장한 여행 목록 조회")
    public ResponseEntity<List<SavedTripResponse>> getSavedTrips(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(savedTripService.getSavedTrips(userId));
    }

    @DeleteMapping("/{savedTripId}")
    @Operation(summary = "저장한 여행 기록 삭제 (복사된 내 계획은 유지)")
    public ResponseEntity<Void> unsave(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID savedTripId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        savedTripService.unsave(userId, savedTripId);
        return ResponseEntity.noContent().build();
    }
}
