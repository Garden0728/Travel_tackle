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
import Timeout.travel_tackle.trip.dto.TripSummaryResponse;
import Timeout.travel_tackle.trip.repository.SavedTripRepository;
import Timeout.travel_tackle.trip.repository.TripDayRepository;
import Timeout.travel_tackle.trip.repository.TripItemRepository;
import Timeout.travel_tackle.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedTripService {

    private final SavedTripRepository savedTripRepository;
    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final TripItemRepository tripItemRepository;
    private final UserRepository userRepository;

    /**
     * 다른 사용자의 공개 여행을 내 계획으로 저장(복사).
     * 원본 Trip/TripDay/TripItem을 통째로 복제해 내 소유의 새 Trip을 만든다.
     */
    @Transactional
    public TripSummaryResponse save(UUID userId, UUID originalTripId) {
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

        // === [과금 보류] 크레딧/무료 횟수 차감 훅 지점 ===
        // 정책 확정 시 여기서 user.creditBalance 검증·차감(또는 freeTrialsUsed) +
        // CreditTransaction 기록을 수행한다. 현재는 과금 없이 저장만 동작.

        Trip copy = new Trip(user, original.getTitle(), original.getStartDate(), original.getEndDate());
        tripRepository.saveAndFlush(copy);
        copyDaysAndItems(original, copy);

        savedTripRepository.save(new SavedTrip(user, original));
        return TripSummaryResponse.from(copy);
    }

    @Transactional(readOnly = true)
    public List<SavedTripResponse> getSavedTrips(UUID userId) {
        User user = findUser(userId);
        return savedTripRepository.findAllWithOriginalByUser(user)
                .stream().map(SavedTripResponse::from).toList();
    }

    @Transactional
    public void unsave(UUID userId, UUID savedTripId) {
        User user = findUser(userId);
        SavedTrip savedTrip = savedTripRepository.findByIdAndUser(savedTripId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.SAVED_TRIP_NOT_FOUND));
        savedTripRepository.delete(savedTrip);
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
                            item.getStartTime(),
                            item.getEndTime(),
                            item.getOrderIndex()))
                    .toList();
            tripItemRepository.saveAll(copiedItems);
        }
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
    }
}
