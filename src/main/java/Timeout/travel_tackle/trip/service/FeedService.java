package Timeout.travel_tackle.trip.service;

import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.trip.dto.FeedItemResponse;
import Timeout.travel_tackle.trip.dto.PublicTripDetailResponse;
import Timeout.travel_tackle.trip.dto.TripDetailResponse;
import Timeout.travel_tackle.trip.dto.TripRecordResponse;
import Timeout.travel_tackle.trip.repository.TripFeedbackRepository;
import Timeout.travel_tackle.trip.repository.TripPhotoRepository;
import Timeout.travel_tackle.trip.repository.TripQueryRepository;
import Timeout.travel_tackle.trip.repository.TripRecordRepository;
import Timeout.travel_tackle.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final TripRepository tripRepository;
    private final TripRecordRepository tripRecordRepository;
    private final TripPhotoRepository tripPhotoRepository;
    private final TripQueryRepository tripQueryRepository;
    private final TripFeedbackRepository tripFeedbackRepository;

    /**
     * 공개된 여행 피드 — 최신순 페이지네이션.
     * 썸네일은 TripRecord의 첫 사진, feedbackCount는 batch 집계.
     */
    @Transactional(readOnly = true)
    public Page<FeedItemResponse> getFeed(Pageable pageable) {
        Page<Trip> trips = tripRepository.findPublishedWithUser(pageable);
        List<UUID> tripIds = trips.getContent().stream().map(Trip::getId).toList();
        Map<UUID, String> thumbnails = resolveThumbnails(trips.getContent());
        Map<UUID, Long> feedbackCounts = resolveFeedbackCounts(tripIds);
        return trips.map(trip -> FeedItemResponse.of(
                trip,
                thumbnails.get(trip.getId()),
                feedbackCounts.getOrDefault(trip.getId(), 0L)));
    }

    /**
     * 공개된 여행의 상세(일정 + 기록 + 작성자 + 피드백 수) 조회.
     */
    @Transactional(readOnly = true)
    public PublicTripDetailResponse getPublicTripDetail(UUID tripId) {
        Trip trip = tripRepository.findPublishedDetailById(tripId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_PUBLISHED));

        TripDetailResponse detail = tripQueryRepository.findDetail(trip);
        TripRecordResponse record = tripRecordRepository.findByTrip(trip)
                .map(r -> TripRecordResponse.of(r,
                        tripPhotoRepository.findAllByRecordOrderByUploadedAtAsc(r)))
                .orElse(null);

        long feedbackCount = tripFeedbackRepository.countGroupByTripIds(List.of(tripId))
                .stream().findFirst().map(row -> (Long) row[1]).orElse(0L);

        return PublicTripDetailResponse.of(trip, detail.days(), record, feedbackCount);
    }

    private Map<UUID, String> resolveThumbnails(List<Trip> trips) {
        if (trips.isEmpty()) {
            return Map.of();
        }
        List<UUID> tripIds = trips.stream().map(Trip::getId).toList();
        Map<UUID, String> thumbnails = new HashMap<>();
        for (Object[] row : tripPhotoRepository.findThumbnailRowsByTripIds(tripIds)) {
            thumbnails.putIfAbsent((UUID) row[0], (String) row[1]);
        }
        return thumbnails;
    }

    private Map<UUID, Long> resolveFeedbackCounts(List<UUID> tripIds) {
        if (tripIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : tripFeedbackRepository.countGroupByTripIds(tripIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }
}
