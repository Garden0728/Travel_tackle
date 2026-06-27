package Timeout.travel_tackle.trip.service;

import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.trip.dto.FeedItemResponse;
import Timeout.travel_tackle.trip.dto.PublicTripDetailResponse;
import Timeout.travel_tackle.trip.dto.TripDetailResponse;
import Timeout.travel_tackle.trip.dto.TripRecordResponse;
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

    /**
     * 공개된 여행 피드 — 최신순 페이지네이션.
     * 각 여행의 대표 썸네일은 기록(TripRecord)의 가장 먼저 업로드된 사진을 사용한다.
     */
    @Transactional(readOnly = true)
    public Page<FeedItemResponse> getFeed(Pageable pageable) {
        Page<Trip> trips = tripRepository.findPublishedWithUser(pageable);
        Map<UUID, String> thumbnails = resolveThumbnails(trips.getContent());
        return trips.map(trip -> FeedItemResponse.of(trip, thumbnails.get(trip.getId())));
    }

    /**
     * 공개된 여행의 상세(일정 + 기록 + 작성자) 조회. 공개 상태가 아니면 조회 불가.
     * 기록이 없는 계획이면 record는 null.
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

        return PublicTripDetailResponse.of(trip, detail.days(), record);
    }

    private Map<UUID, String> resolveThumbnails(List<Trip> trips) {
        if (trips.isEmpty()) {
            return Map.of();
        }
        List<UUID> tripIds = trips.stream().map(Trip::getId).toList();
        Map<UUID, String> thumbnails = new HashMap<>();
        // (계획ID, 이미지URL)이 업로드순으로 오므로 계획별 첫 사진만 남긴다
        for (Object[] row : tripPhotoRepository.findThumbnailRowsByTripIds(tripIds)) {
            thumbnails.putIfAbsent((UUID) row[0], (String) row[1]);
        }
        return thumbnails;
    }
}
