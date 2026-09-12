package Timeout.travel_tackle.trip.service;

import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.entity.SavedTrip;
import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.TripDay;
import Timeout.travel_tackle.entity.TripItem;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.trip.dto.SavedTripResponse;
import Timeout.travel_tackle.trip.dto.TripDetailResponse;
import Timeout.travel_tackle.trip.dto.TripSummaryResponse;
import Timeout.travel_tackle.trip.repository.SavedTripRepository;
import Timeout.travel_tackle.trip.repository.TripDayRepository;
import Timeout.travel_tackle.trip.repository.TripFeedbackRepository;
import Timeout.travel_tackle.trip.repository.TripItemRepository;
import Timeout.travel_tackle.trip.repository.TripPhotoRepository;
import Timeout.travel_tackle.trip.repository.TripQueryRepository;
import Timeout.travel_tackle.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedTripService {

    private final SavedTripRepository savedTripRepository;
    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final TripItemRepository tripItemRepository;
    private final TripQueryRepository tripQueryRepository;
    private final TripPhotoRepository tripPhotoRepository;
    private final TripFeedbackRepository tripFeedbackRepository;
    private final UserRepository userRepository;

    /**
     * 다른 사용자의 공개 여행을 스크랩(찜)한다. 이 시점엔 원본을 복사하지 않는다 —
     * 실제 내 계획으로 복사하는 건 {@link #copy(UUID, UUID)}에서 별도로 한다.
     */
    @Transactional
    public SavedTripResponse save(UUID userId, UUID originalTripId) {
        User user = findUser(userId);
        Trip original = tripRepository.findById(originalTripId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));

        if (!original.isPublished()) {
            throw new CustomException(ErrorCode.TRIP_NOT_PUBLISHED);
        }
        if (original.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.CANNOT_SAVE_OWN_TRIP);
        }
        if (savedTripRepository.existsByUserAndOriginalTrip(user, original)) {
            throw new CustomException(ErrorCode.TRIP_ALREADY_SAVED);
        }

        SavedTrip savedTrip = savedTripRepository.save(new SavedTrip(user, original));
        return toResponse(savedTrip);
    }

    /**
     * 스크랩해 둔 여행을 실제로 내 계획으로 복사한다. 원본 Trip/TripDay/TripItem을 통째로 복제해
     * 내 소유의 새 Trip을 만든다. 이미 복사한 적이 있으면 새로 복사하지 않고 기존 복사본을 그대로 반환한다.
     */
    @Transactional
    public TripSummaryResponse copy(UUID userId, UUID savedTripId) {
        User user = findUser(userId);
        SavedTrip savedTrip = savedTripRepository.findByIdAndUser(savedTripId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.SAVED_TRIP_NOT_FOUND));

        if (savedTrip.getCopiedTrip() != null) {
            return TripSummaryResponse.from(savedTrip.getCopiedTrip());
        }

        Trip original = savedTrip.getOriginalTrip();

        // === [과금 보류] 크레딧/무료 횟수 차감 훅 지점 ===
        // 정책 확정 시 여기서 user.creditBalance 검증·차감(또는 freeTrialsUsed) +
        // CreditTransaction 기록을 수행한다. 현재는 과금 없이 복사만 동작.

        Trip copy = new Trip(user, original.getTitle(), original.getStartDate(), original.getEndDate());
        tripRepository.saveAndFlush(copy);
        copyDaysAndItems(original, copy);

        savedTrip.markCopied(copy);
        return TripSummaryResponse.from(copy);
    }

    @Transactional(readOnly = true)
    public List<SavedTripResponse> getSavedTrips(UUID userId) {
        User user = findUser(userId);
        List<SavedTrip> savedTrips = savedTripRepository.findAllWithOriginalByUser(user);
        return enrich(savedTrips);
    }

    @Transactional
    public void unsave(UUID userId, UUID savedTripId) {
        User user = findUser(userId);
        SavedTrip savedTrip = savedTripRepository.findByIdAndUser(savedTripId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.SAVED_TRIP_NOT_FOUND));
        savedTripRepository.delete(savedTrip);
    }

    private SavedTripResponse toResponse(SavedTrip savedTrip) {
        return enrich(List.of(savedTrip)).getFirst();
    }

    private List<SavedTripResponse> enrich(List<SavedTrip> savedTrips) {
        if (savedTrips.isEmpty()) {
            return List.of();
        }
        List<Trip> originals = savedTrips.stream().map(SavedTrip::getOriginalTrip).toList();
        List<UUID> tripIds = originals.stream().map(Trip::getId).toList();

        Map<UUID, String> thumbnails = resolveThumbnails(tripIds);
        Map<UUID, Long> feedbackCounts = resolveFeedbackCounts(tripIds);
        Map<UUID, Long> saveCounts = resolveSaveCounts(tripIds);

        return savedTrips.stream()
                .map(savedTrip -> {
                    UUID tripId = savedTrip.getOriginalTrip().getId();
                    TripDetailResponse detail = tripQueryRepository.findDetail(savedTrip.getOriginalTrip());
                    String region = resolveRegion(detail);
                    return SavedTripResponse.of(
                            savedTrip,
                            region,
                            thumbnails.get(tripId),
                            feedbackCounts.getOrDefault(tripId, 0L),
                            saveCounts.getOrDefault(tripId, 0L)
                    );
                })
                .toList();
    }

    private String resolveRegion(TripDetailResponse detail) {
        return detail.days().stream()
                .flatMap(day -> day.items().stream())
                .findFirst()
                .map(item -> RegionLabelResolver.fromAddress(item.address()))
                .orElse(null);
    }

    private Map<UUID, String> resolveThumbnails(List<UUID> tripIds) {
        Map<UUID, String> thumbnails = new HashMap<>();
        for (Object[] row : tripPhotoRepository.findThumbnailRowsByTripIds(tripIds)) {
            thumbnails.putIfAbsent((UUID) row[0], (String) row[1]);
        }
        return thumbnails;
    }

    private Map<UUID, Long> resolveFeedbackCounts(List<UUID> tripIds) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : tripFeedbackRepository.countGroupByTripIds(tripIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    private Map<UUID, Long> resolveSaveCounts(List<UUID> tripIds) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : savedTripRepository.countGroupByOriginalTripIds(tripIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    private void copyDaysAndItems(Trip original, Trip copy) {
        List<TripDay> originalDays = tripDayRepository.findAllByTripOrderByDayNumber(original);
        for (TripDay originalDay : originalDays) {
            TripDay copiedDay = new TripDay(copy, originalDay.getDayNumber(), originalDay.getDate());
            tripDayRepository.save(copiedDay);

            List<TripItem> copiedItems = tripItemRepository
                    .findAllByTripDayOrderByOrderIndex(originalDay).stream()
                    .map(item -> new TripItem(
                            copiedDay,
                            item.getTourApiContentId(),
                            item.getCachedTitle(),
                            item.getCachedImageUrl(),
                            item.getRegionCode(),
                            item.getContentTypeId(),
                            item.getLclsSystm1(),
                            item.getLclsSystm2(),
                            item.getLclsSystm3(),
                            item.getStartTime(),
                            item.getEndTime(),
                            item.getOrderIndex(),
                            item.getAddress(),
                            item.getMemo()))
                    .toList();
            tripItemRepository.saveAll(copiedItems);
        }
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
    }
}
