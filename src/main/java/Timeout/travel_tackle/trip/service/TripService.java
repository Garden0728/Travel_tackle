package Timeout.travel_tackle.trip.service;

import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.TripDay;
import Timeout.travel_tackle.entity.TripItem;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.trip.dto.TripDetailResponse;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.trip.dto.*;
import Timeout.travel_tackle.trip.repository.TripDayRepository;
import Timeout.travel_tackle.trip.repository.TripItemRepository;
import Timeout.travel_tackle.trip.repository.TripQueryRepository;
import Timeout.travel_tackle.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final TripItemRepository tripItemRepository;
    private final TripQueryRepository tripQueryRepository;
    private final UserRepository userRepository;

    @Transactional
    public TripSummaryResponse createTrip(UUID userId, CreateTripRequest request) {
        User user = findUser(userId);
        Trip trip = new Trip(user, request.title(), request.startDate(), request.endDate());
        tripRepository.saveAndFlush(trip);
        generateTripDays(trip, request.startDate(), request.endDate());
        return TripSummaryResponse.from(trip);
    }

    @Transactional(readOnly = true)
    public List<TripSummaryResponse> getMyTrips(UUID userId) {
        User user = findUser(userId);
        return tripRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream().map(TripSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TripDetailResponse getTripDetail(UUID userId, UUID tripId) {
        Trip trip = findTripOwnedBy(userId, tripId);
        return tripQueryRepository.findDetail(trip);
    }

    @Transactional
    public TripSummaryResponse updateTrip(UUID userId, UUID tripId, UpdateTripRequest request) {
        Trip trip = findTripOwnedBy(userId, tripId);
        boolean datesChanged = !trip.getStartDate().equals(request.startDate())
                || !trip.getEndDate().equals(request.endDate());

        trip.updateSchedule(request.title(), request.startDate(), request.endDate());

        if (datesChanged) {
            deleteAllDaysAndItems(trip);
            generateTripDays(trip, request.startDate(), request.endDate());
        }

        return TripSummaryResponse.from(trip);
    }

    @Transactional
    public void deleteTrip(UUID userId, UUID tripId) {
        Trip trip = findTripOwnedBy(userId, tripId);
        deleteAllDaysAndItems(trip);
        tripRepository.delete(trip);
    }

    @Transactional
    public TripItemResponse addTripItem(UUID userId, UUID tripId, UUID dayId, AddTripItemRequest request) {
        Trip trip = findTripOwnedBy(userId, tripId);
        TripDay day = findDayInTrip(dayId, trip);
        List<TripItem> existing = tripItemRepository.findAllByTripDayOrderByOrderIndex(day);
        int nextIndex = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getOrderIndex() + 1;
        TripItem item = new TripItem(day, request.tourApiContentId(), request.cachedTitle(),
                request.cachedImageUrl(), request.startTime(), request.endTime(), nextIndex);
        tripItemRepository.save(item);
        return TripItemResponse.from(item);
    }

    @Transactional
    public TripItemResponse updateTripItem(UUID userId, UUID tripId, UUID dayId, UUID itemId,
                                           UpdateTripItemRequest request) {
        Trip trip = findTripOwnedBy(userId, tripId);
        TripDay day = findDayInTrip(dayId, trip);
        TripItem item = findItemInDay(itemId, day);
        item.changeTime(request.startTime(), request.endTime());
        return TripItemResponse.from(item);
    }

    @Transactional
    public void deleteTripItem(UUID userId, UUID tripId, UUID dayId, UUID itemId) {
        Trip trip = findTripOwnedBy(userId, tripId);
        TripDay day = findDayInTrip(dayId, trip);
        TripItem item = findItemInDay(itemId, day);
        tripItemRepository.delete(item);
    }

    /**
     * 드래그앤드롭 같은 날 순서 변경.
     * unique(trip_day_id, order_index) 제약 충돌 방지를 위해 2단계 업데이트:
     *   1단계) 기존 index를 size 이상의 임시값으로 올려서 충돌 없이 flush
     *   2단계) 최종 index(0, 1, 2, ...)로 설정
     */
    @Transactional
    public List<TripItemResponse> reorderTripItems(UUID userId, UUID tripId, UUID dayId,
                                                   ReorderTripItemsRequest request) {
        Trip trip = findTripOwnedBy(userId, tripId);
        TripDay day = findDayInTrip(dayId, trip);
        List<TripItem> existing = tripItemRepository.findAllByTripDayOrderByOrderIndex(day);

        Set<UUID> dayItemIds = existing.stream().map(TripItem::getId).collect(Collectors.toSet());
        for (UUID id : request.itemIds()) {
            if (!dayItemIds.contains(id)) {
                throw new CustomException(ErrorCode.TRIP_ITEM_NOT_FOUND);
            }
        }

        Map<UUID, TripItem> itemMap = existing.stream()
                .collect(Collectors.toMap(TripItem::getId, i -> i));

        int size = request.itemIds().size();

        // 1단계: 임시 큰 값으로 올려서 충돌 방지
        for (int i = 0; i < size; i++) {
            itemMap.get(request.itemIds().get(i)).changeOrder(size + i);
        }
        tripItemRepository.saveAll(existing);
        tripItemRepository.flush();

        // 2단계: 최종 순서 적용
        List<TripItem> ordered = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TripItem item = itemMap.get(request.itemIds().get(i));
            item.changeOrder(i);
            ordered.add(item);
        }
        tripItemRepository.saveAll(ordered);

        return ordered.stream().map(TripItemResponse::from).toList();
    }

    /**
     * 드래그앤드롭으로 다른 날(TripDay)로 관광지 이동.
     * newOrderIndex가 null이면 대상 일차 맨 끝에 추가.
     */
    @Transactional
    public TripItemResponse moveTripItem(UUID userId, UUID tripId, UUID itemId,
                                         MoveTripItemRequest request) {
        Trip trip = findTripOwnedBy(userId, tripId);
        TripItem item = tripItemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_ITEM_NOT_FOUND));
        if (!item.getTripDay().getTrip().getId().equals(tripId)) {
            throw new CustomException(ErrorCode.TRIP_ACCESS_DENIED);
        }

        TripDay newDay = findDayInTrip(request.newDayId(), trip);
        int newIndex;
        if (request.newOrderIndex() != null) {
            newIndex = request.newOrderIndex();
        } else {
            List<TripItem> targetItems = tripItemRepository.findAllByTripDayOrderByOrderIndex(newDay);
            newIndex = targetItems.isEmpty() ? 0 : targetItems.get(targetItems.size() - 1).getOrderIndex() + 1;
        }

        item.moveTo(newDay, newIndex);
        return TripItemResponse.from(item);
    }

    // --- 내부 헬퍼 ---

    private void generateTripDays(Trip trip, LocalDate start, LocalDate end) {
        List<TripDay> days = new ArrayList<>();
        LocalDate cursor = start;
        int dayNumber = 1;
        while (!cursor.isAfter(end)) {
            days.add(new TripDay(trip, dayNumber++, cursor));
            cursor = cursor.plusDays(1);
        }
        tripDayRepository.saveAll(days);
    }

    private void deleteAllDaysAndItems(Trip trip) {
        tripQueryRepository.bulkDeleteByTrip(trip);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
    }

    private Trip findTripOwnedBy(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));
        if (!trip.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.TRIP_ACCESS_DENIED);
        }
        return trip;
    }

    private TripDay findDayInTrip(UUID dayId, Trip trip) {
        TripDay day = tripDayRepository.findById(dayId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_DAY_NOT_FOUND));
        if (!day.getTrip().getId().equals(trip.getId())) {
            throw new CustomException(ErrorCode.TRIP_DAY_NOT_FOUND);
        }
        return day;
    }

    private TripItem findItemInDay(UUID itemId, TripDay day) {
        TripItem item = tripItemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_ITEM_NOT_FOUND));
        if (!item.getTripDay().getId().equals(day.getId())) {
            throw new CustomException(ErrorCode.TRIP_ITEM_NOT_FOUND);
        }
        return item;
    }
}
