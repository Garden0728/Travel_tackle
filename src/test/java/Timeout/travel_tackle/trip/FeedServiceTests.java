package Timeout.travel_tackle.trip;

import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.trip.dto.CreateFeedbackRequest;
import Timeout.travel_tackle.trip.dto.CreateTripRequest;
import Timeout.travel_tackle.trip.dto.FeedItemResponse;
import Timeout.travel_tackle.trip.dto.FeedItemType;
import Timeout.travel_tackle.trip.dto.FeedSort;
import Timeout.travel_tackle.trip.dto.TripRecordRequest;
import Timeout.travel_tackle.trip.service.FeedService;
import Timeout.travel_tackle.trip.service.SavedTripService;
import Timeout.travel_tackle.trip.service.TripFeedbackService;
import Timeout.travel_tackle.trip.service.TripRecordService;
import Timeout.travel_tackle.trip.service.TripService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class FeedServiceTests {

    @Autowired FeedService feedService;
    @Autowired TripService tripService;
    @Autowired TripFeedbackService feedbackService;
    @Autowired TripRecordService tripRecordService;
    @Autowired SavedTripService savedTripService;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager entityManager;

    private User owner;
    private User reviewerA;
    private User reviewerB;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User("owner@feed.test", "계획자", "KR"));
        reviewerA = userRepository.save(new User("a@feed.test", "리뷰어A", "KR"));
        reviewerB = userRepository.save(new User("b@feed.test", "리뷰어B", "KR"));
    }

    @Test
    void popularSortOrdersByFeedbackCountThenCreatedAtDesc() {
        UUID oldest = createPublishedTrip("첫 번째");   // 참견 1
        UUID middle = createPublishedTrip("두 번째");   // 참견 0
        UUID newest = createPublishedTrip("세 번째");   // 참견 0 — middle 과 동점이면 최신이 앞
        UUID popular = createPublishedTrip("네 번째"); // 참견 2

        giveFeedback(reviewerA, oldest);
        giveFeedback(reviewerA, popular);
        giveFeedback(reviewerB, popular);
        entityManager.flush();
        entityManager.clear();

        List<UUID> order = feedService.getFeed(PageRequest.of(0, 10), FeedSort.POPULAR)
                .getContent().stream().map(FeedItemResponse::tripId).toList();

        assertEquals(List.of(popular, oldest, newest, middle), order);
    }

    @Test
    void latestSortKeepsCreatedAtDescOrder() {
        UUID first = createPublishedTrip("첫 번째");
        UUID second = createPublishedTrip("두 번째");
        giveFeedback(reviewerA, first);
        entityManager.flush();
        entityManager.clear();

        Page<FeedItemResponse> feed = feedService.getFeed(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), FeedSort.LATEST);

        assertEquals(List.of(second, first),
                feed.getContent().stream().map(FeedItemResponse::tripId).toList());
    }

    @Test
    void feedItemsCarrySaveCountForPlanAndRecordCards() {
        UUID tripId = createPublishedTrip("저장되는 여행");
        tripRecordService.createRecord(owner.getId(), tripId,
                new TripRecordRequest("기록", "후기",
                        List.of(new TripRecordRequest.PhotoEntry("https://cdn.test/p1.jpg", "캡션"))));
        savedTripService.save(reviewerA.getId(), tripId);
        savedTripService.save(reviewerB.getId(), tripId);
        entityManager.flush();
        entityManager.clear();

        List<FeedItemResponse> items = feedService.getFeed(PageRequest.of(0, 10), FeedSort.POPULAR).getContent();

        assertEquals(2, items.size());
        assertEquals(FeedItemType.PLAN, items.get(0).type());
        assertEquals(2, items.get(0).saveCount());
        assertEquals(FeedItemType.RECORD, items.get(1).type());
        assertEquals(2, items.get(1).saveCount());
    }

    @Test
    void unknownSortValueIsRejected() {
        CustomException ex = assertThrows(CustomException.class, () -> FeedSort.from("trending"));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertEquals(FeedSort.POPULAR, FeedSort.from("popular"));
        assertEquals(FeedSort.LATEST, FeedSort.from("LATEST"));
    }

    private UUID createPublishedTrip(String title) {
        LocalDate date = LocalDate.of(2026, 7, 1);
        UUID tripId = tripService.createTrip(owner.getId(), new CreateTripRequest(title, date, date)).id();
        tripService.publishTrip(owner.getId(), tripId);
        entityManager.flush();
        return tripId;
    }

    private void giveFeedback(User reviewer, UUID tripId) {
        feedbackService.create(reviewer.getId(), tripId,
                new CreateFeedbackRequest("참견합니다", null, null, List.of()));
    }
}
