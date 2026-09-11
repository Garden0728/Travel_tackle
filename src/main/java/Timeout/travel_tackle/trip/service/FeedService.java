package Timeout.travel_tackle.trip.service;

import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.TripRecord;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.trip.dto.FeedItemResponse;
import Timeout.travel_tackle.trip.dto.FeedSort;
import Timeout.travel_tackle.trip.dto.PublicTripDetailResponse;
import Timeout.travel_tackle.trip.dto.TripDetailResponse;
import Timeout.travel_tackle.trip.dto.TripRecordResponse;
import Timeout.travel_tackle.trip.repository.SavedTripRepository;
import Timeout.travel_tackle.trip.repository.TripFeedbackRepository;
import Timeout.travel_tackle.trip.repository.TripPhotoRepository;
import Timeout.travel_tackle.trip.repository.TripQueryRepository;
import Timeout.travel_tackle.trip.repository.TripRecordRepository;
import Timeout.travel_tackle.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final TripRepository tripRepository;
    private final TripRecordRepository tripRecordRepository;
    private final TripPhotoRepository tripPhotoRepository;
    private final TripQueryRepository tripQueryRepository;
    private final TripFeedbackRepository tripFeedbackRepository;
    private final SavedTripRepository savedTripRepository;

    /**
     * 공개된 여행 피드 — 최신순(LATEST) 또는 참견 수순(POPULAR) 페이지네이션.
     * Trip 하나당 PLAN 카드 1개는 항상, 기록(TripRecord)이 있으면 RECORD 카드를 추가로 낸다.
     * 그래서 응답 개수가 요청한 size보다 많을 수 있다 (Trip 1개 -> 최대 2개 항목) — 실사용자 규모가
     * 커지면 페이지네이션을 다시 손봐야 하는 알려진 한계.
     */
    @Transactional(readOnly = true)
    public Page<FeedItemResponse> getFeed(Pageable pageable, FeedSort sort) {
        return getFeed(pageable, sort, null);
    }

    /**
     * keyword가 있으면 계획 제목/기록 제목·내용을 검색한 결과를 sort(RELEVANCE 기본)로 정렬해 반환한다.
     */
    @Transactional(readOnly = true)
    public Page<FeedItemResponse> getFeed(Pageable pageable, FeedSort sort, String keyword) {
        Page<Trip> trips = StringUtils.hasText(keyword)
                ? tripQueryRepository.searchPublishedTrips(keyword.trim(), sort, pageable)
                : (sort == FeedSort.POPULAR
                        ? tripRepository.findPublishedWithUserOrderByFeedbackCount(pageable)
                        : tripRepository.findPublishedWithUser(pageable));
        List<UUID> tripIds = trips.getContent().stream().map(Trip::getId).toList();
        Map<UUID, String> thumbnails = resolveThumbnails(trips.getContent());
        Map<UUID, Long> feedbackCounts = resolveFeedbackCounts(tripIds);
        Map<UUID, Long> saveCounts = resolveSaveCounts(tripIds);
        Map<UUID, TripRecord> records = resolveRecords(tripIds);

        List<FeedItemResponse> items = trips.getContent().stream()
                .flatMap(trip -> buildFeedItems(trip, thumbnails, feedbackCounts, saveCounts, records).stream())
                .toList();

        return new PageImpl<>(items, pageable, trips.getTotalElements());
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

        return PublicTripDetailResponse.of(trip, resolveRegion(detail), detail.days(), record, feedbackCount);
    }

    private List<FeedItemResponse> buildFeedItems(
            Trip trip, Map<UUID, String> thumbnails, Map<UUID, Long> feedbackCounts,
            Map<UUID, Long> saveCounts, Map<UUID, TripRecord> records
    ) {
        String thumbnailUrl = thumbnails.get(trip.getId());
        long feedbackCount = feedbackCounts.getOrDefault(trip.getId(), 0L);
        long saveCount = saveCounts.getOrDefault(trip.getId(), 0L);
        TripDetailResponse detail = tripQueryRepository.findDetail(trip);
        String region = resolveRegion(detail);

        List<FeedItemResponse> items = new ArrayList<>();
        items.add(FeedItemResponse.ofPlan(trip, thumbnailUrl, feedbackCount, saveCount, region, detail.days()));

        TripRecord record = records.get(trip.getId());
        if (record != null) {
            items.add(FeedItemResponse.ofRecord(trip, record, thumbnailUrl, feedbackCount, saveCount, region));
        }
        return items;
    }

    private String resolveRegion(TripDetailResponse detail) {
        return detail.days().stream()
                .flatMap(day -> day.items().stream())
                .findFirst()
                .map(item -> RegionLabelResolver.fromAddress(item.address()))
                .orElse(null);
    }

    private Map<UUID, TripRecord> resolveRecords(List<UUID> tripIds) {
        if (tripIds.isEmpty()) {
            return Map.of();
        }
        return tripRecordRepository.findPublishedByTripIdInWithTripAndUser(tripIds).stream()
                .collect(Collectors.toMap(r -> r.getTrip().getId(), r -> r));
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

    private Map<UUID, Long> resolveSaveCounts(List<UUID> tripIds) {
        if (tripIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : savedTripRepository.countGroupByOriginalTripIds(tripIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
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
