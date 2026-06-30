package Timeout.travel_tackle.trip.service;

import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.cart.CartService;
import Timeout.travel_tackle.cart.CartService.CartItemResponse;
import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.TripDay;
import Timeout.travel_tackle.entity.TripFeedback;
import Timeout.travel_tackle.entity.TripFeedbackRecommendation;
import Timeout.travel_tackle.entity.TripItem;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.tour.dto.TourDtos.ContentDetail;
import Timeout.travel_tackle.tour.service.TourService;
import Timeout.travel_tackle.trip.dto.CreateFeedbackRequest;
import Timeout.travel_tackle.trip.dto.FeedbackRecommendationResponse;
import Timeout.travel_tackle.trip.dto.FeedbackResponse;
import Timeout.travel_tackle.trip.dto.ReceivedFeedbackSummary;
import Timeout.travel_tackle.trip.dto.UpdateFeedbackRequest;
import Timeout.travel_tackle.trip.repository.TripDayRepository;
import Timeout.travel_tackle.trip.repository.TripFeedbackRecommendationRepository;
import Timeout.travel_tackle.trip.repository.TripFeedbackRepository;
import Timeout.travel_tackle.trip.repository.TripItemRepository;
import Timeout.travel_tackle.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripFeedbackService {

    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final TripItemRepository tripItemRepository;
    private final TripFeedbackRepository feedbackRepository;
    private final TripFeedbackRecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final TourService tourService;
    private final CartService cartService;

    @Transactional
    public FeedbackResponse create(UUID userId, UUID tripId, CreateFeedbackRequest request) {
        Trip trip = findPublishedTrip(tripId);
        User author = findUser(userId);

        if (trip.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.CANNOT_FEEDBACK_OWN_TRIP);
        }
        if (request.tripDayId() != null && request.tripItemId() != null) {
            throw new CustomException(ErrorCode.FEEDBACK_TARGET_CONFLICT);
        }

        TripDay tripDay = resolveDay(request.tripDayId(), trip);
        TripItem tripItem = resolveItem(request.tripItemId(), trip);

        TripFeedback feedback = feedbackRepository.save(
                new TripFeedback(trip, tripDay, tripItem, author, request.content()));

        List<TripFeedbackRecommendation> recs = buildRecommendations(feedback, request.recommendations());
        recommendationRepository.saveAll(recs);

        return toResponse(feedback, recs);
    }

    @Transactional
    public Page<FeedbackResponse> getList(UUID tripId, UUID dayId, UUID itemId,
                                          UUID callerId, Pageable pageable) {
        Trip trip = findTrip(tripId);

        if (!trip.isPublished()) {
            boolean isOwner = callerId != null && trip.getUser().getId().equals(callerId);
            boolean isFeedbackAuthor = callerId != null
                    && feedbackRepository.existsByTripIdAndAuthorId(tripId, callerId);
            if (!isOwner && !isFeedbackAuthor) {
                throw new CustomException(ErrorCode.TRIP_NOT_PUBLISHED);
            }
        }

        // 소유자가 조회하면 먼저 읽음 처리 후 DB에서 새로 조회 (isRead 응답값 일관성)
        if (callerId != null && trip.getUser().getId().equals(callerId)) {
            feedbackRepository.markAllReadByTripId(tripId);
        }

        Page<TripFeedback> page = fetchPage(tripId, dayId, itemId, pageable);

        return page.map(f -> toResponse(f, recommendationRepository.findAllByFeedbackId(f.getId())));
    }

    @Transactional
    public Page<FeedbackResponse> getAll(UUID tripId, UUID callerId, Pageable pageable) {
        Trip trip = findTrip(tripId);

        if (!trip.isPublished()) {
            boolean isOwner = callerId != null && trip.getUser().getId().equals(callerId);
            boolean isFeedbackAuthor = callerId != null
                    && feedbackRepository.existsByTripIdAndAuthorId(tripId, callerId);
            if (!isOwner && !isFeedbackAuthor) {
                throw new CustomException(ErrorCode.TRIP_NOT_PUBLISHED);
            }
        }

        if (callerId != null && trip.getUser().getId().equals(callerId)) {
            feedbackRepository.markAllReadByTripId(tripId);
        }

        return feedbackRepository.findAllByTripId(tripId, pageable)
                .map(f -> toResponse(f, recommendationRepository.findAllByFeedbackId(f.getId())));
    }

    @Transactional
    public FeedbackResponse update(UUID userId, UUID tripId, UUID feedbackId,
                                   UpdateFeedbackRequest request) {
        TripFeedback feedback = findFeedbackInTrip(feedbackId, tripId);
        if (!feedback.getAuthor().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FEEDBACK_ACCESS_DENIED);
        }

        feedback.updateContent(request.content());

        // 추천 관광지 전체 교체
        recommendationRepository.deleteAllByFeedbackId(feedbackId);
        List<TripFeedbackRecommendation> recs = buildRecommendations(feedback,
                request.recommendations() != null ? request.recommendations() : List.of());
        recommendationRepository.saveAll(recs);

        return toResponse(feedback, recs);
    }

    @Transactional
    public void delete(UUID userId, UUID tripId, UUID feedbackId) {
        TripFeedback feedback = findFeedbackInTrip(feedbackId, tripId);
        boolean isAuthor = feedback.getAuthor().getId().equals(userId);
        boolean isTripOwner = feedback.getTrip().getUser().getId().equals(userId);
        if (!isAuthor && !isTripOwner) {
            throw new CustomException(ErrorCode.FEEDBACK_ACCESS_DENIED);
        }
        recommendationRepository.deleteAllByFeedbackId(feedbackId);
        feedbackRepository.delete(feedback);
    }

    @Transactional
    public CartItemResponse addRecommendationToCart(UUID userId, UUID tripId, UUID recommendationId) {
        TripFeedbackRecommendation rec = recommendationRepository.findByIdWithTripOwner(recommendationId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_RECOMMENDATION_NOT_FOUND));
        Trip trip = rec.getFeedback().getTrip();
        if (!trip.getId().equals(tripId)) {
            throw new CustomException(ErrorCode.FEEDBACK_RECOMMENDATION_NOT_FOUND);
        }
        if (!trip.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.TRIP_ACCESS_DENIED);
        }
        return cartService.addFromCachedData(userId,
                rec.getTourApiContentId(), rec.getCachedTitle(),
                rec.getCachedImageUrl(), rec.getCachedAreaCode());
    }

    @Transactional(readOnly = true)
    public List<ReceivedFeedbackSummary> getReceivedSummary(UUID userId) {
        User user = findUser(userId);
        List<Trip> myTrips = tripRepository.findAllByUserOrderByCreatedAtDesc(user);
        if (myTrips.isEmpty()) {
            return List.of();
        }

        List<UUID> tripIds = myTrips.stream().map(Trip::getId).toList();

        Map<UUID, Long> totalMap = toMap(feedbackRepository.countGroupByTripIds(tripIds));
        Map<UUID, Long> unreadMap = toMap(feedbackRepository.countUnreadGroupByTripIds(tripIds));
        Map<UUID, LocalDateTime> latestMap = toLatestMap(
                feedbackRepository.findLatestCreatedAtGroupByTripIds(tripIds));

        List<ReceivedFeedbackSummary> result = new ArrayList<>();
        for (Trip trip : myTrips) {
            long total = totalMap.getOrDefault(trip.getId(), 0L);
            if (total == 0) continue;
            result.add(new ReceivedFeedbackSummary(
                    trip.getId(),
                    trip.getTitle(),
                    total,
                    unreadMap.getOrDefault(trip.getId(), 0L),
                    latestMap.get(trip.getId())
            ));
        }
        return result;
    }

    // --- 내부 헬퍼 ---

    private Page<TripFeedback> fetchPage(UUID tripId, UUID dayId, UUID itemId, Pageable pageable) {
        if (itemId != null) {
            return feedbackRepository.findAllByTripItemId(itemId, pageable);
        }
        if (dayId != null) {
            return feedbackRepository.findAllByTripDayId(dayId, pageable);
        }
        return feedbackRepository.findAllByTripIdAndTripDayIsNullAndTripItemIsNull(tripId, pageable);
    }

    private List<TripFeedbackRecommendation> buildRecommendations(
            TripFeedback feedback,
            List<CreateFeedbackRequest.RecommendationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<TripFeedbackRecommendation> recs = new ArrayList<>();
        for (CreateFeedbackRequest.RecommendationRequest req : requests) {
            ContentDetail detail = tourService.getContentDetail(req.contentId());
            recs.add(new TripFeedbackRecommendation(
                    feedback,
                    detail.contentId(),
                    detail.title(),
                    detail.imageUrl(),
                    detail.areaCode()
            ));
        }
        return recs;
    }

    private FeedbackResponse toResponse(TripFeedback feedback,
                                        List<TripFeedbackRecommendation> recs) {
        List<FeedbackRecommendationResponse> recResponses = recs.stream()
                .map(FeedbackRecommendationResponse::from).toList();
        return FeedbackResponse.of(feedback, recResponses);
    }

    private TripDay resolveDay(UUID dayId, Trip trip) {
        if (dayId == null) return null;
        TripDay day = tripDayRepository.findById(dayId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_DAY_NOT_FOUND));
        if (!day.getTrip().getId().equals(trip.getId())) {
            throw new CustomException(ErrorCode.TRIP_DAY_NOT_FOUND);
        }
        return day;
    }

    private TripItem resolveItem(UUID itemId, Trip trip) {
        if (itemId == null) return null;
        TripItem item = tripItemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_ITEM_NOT_FOUND));
        if (!item.getTripDay().getTrip().getId().equals(trip.getId())) {
            throw new CustomException(ErrorCode.TRIP_ITEM_NOT_FOUND);
        }
        return item;
    }

    private Trip findTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));
    }

    private Trip findPublishedTrip(UUID tripId) {
        Trip trip = findTrip(tripId);
        if (!trip.isPublished()) {
            throw new CustomException(ErrorCode.TRIP_NOT_PUBLISHED);
        }
        return trip;
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
    }

    private TripFeedback findFeedbackInTrip(UUID feedbackId, UUID tripId) {
        return feedbackRepository.findByIdAndTripId(feedbackId, tripId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
    }

    private Map<UUID, Long> toMap(List<Object[]> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((UUID) row[0], (Long) row[1]);
        }
        return map;
    }

    private Map<UUID, LocalDateTime> toLatestMap(List<Object[]> rows) {
        Map<UUID, LocalDateTime> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((UUID) row[0], (LocalDateTime) row[1]);
        }
        return map;
    }
}
