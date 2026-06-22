package Timeout.travel_tackle.trip;

import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.cart.CartItemRepository;
import Timeout.travel_tackle.entity.CartItem;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.trip.dto.AddTripItemRequest;
import Timeout.travel_tackle.trip.dto.CreateTripRequest;
import Timeout.travel_tackle.trip.dto.MoveTripItemRequest;
import Timeout.travel_tackle.trip.dto.ReorderTripItemsRequest;
import Timeout.travel_tackle.trip.dto.TripDayResponse;
import Timeout.travel_tackle.trip.dto.TripDetailResponse;
import Timeout.travel_tackle.trip.dto.TripItemResponse;
import Timeout.travel_tackle.trip.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class TripServiceTests {

    @Autowired TripService tripService;
    @Autowired UserRepository userRepository;
    @Autowired CartItemRepository cartItemRepository;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("trip-test@example.com", "여행자", "KR"));
        userId = user.getId();
    }

    @Test
    void addsOwnedCartItemToTripUsingSavedSnapshot() {
        TripDetailResponse trip = createTripWithDays(1);
        TripDayResponse day = trip.days().getFirst();
        CartItem cartItem = saveCartItem(user, "125266", "경복궁", "image.jpg");

        TripItemResponse result = tripService.addTripItem(userId, trip.id(), day.id(),
                new AddTripItemRequest(cartItem.getId(), null, null));

        assertEquals("125266", result.tourApiContentId());
        assertEquals("경복궁", result.cachedTitle());
        assertEquals("image.jpg", result.cachedImageUrl());
    }

    @Test
    void rejectsAnotherUsersCartItem() {
        TripDetailResponse trip = createTripWithDays(1);
        TripDayResponse day = trip.days().getFirst();
        User anotherUser = userRepository.save(new User("other@example.com", "다른 사용자", "KR"));
        CartItem cartItem = saveCartItem(anotherUser, "999", "다른 장바구니", null);

        CustomException exception = assertThrows(CustomException.class, () ->
                tripService.addTripItem(userId, trip.id(), day.id(),
                        new AddTripItemRequest(cartItem.getId(), null, null)));

        assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void rejectsPartialReorderRequest() {
        TripDetailResponse trip = createTripWithDays(1);
        TripDayResponse day = trip.days().getFirst();
        TripItemResponse first = addItem(trip.id(), day.id(), "1");
        addItem(trip.id(), day.id(), "2");

        CustomException exception = assertThrows(CustomException.class, () ->
                tripService.reorderTripItems(userId, trip.id(), day.id(),
                        new ReorderTripItemsRequest(List.of(first.id()))));

        assertEquals(ErrorCode.INVALID_TRIP_ITEM_ORDER, exception.getErrorCode());
    }

    @Test
    void movesItemIntoOccupiedIndexAndNormalizesBothDays() {
        TripDetailResponse trip = createTripWithDays(2);
        TripDayResponse firstDay = trip.days().get(0);
        TripDayResponse secondDay = trip.days().get(1);
        addItem(trip.id(), firstDay.id(), "A");
        TripItemResponse movingItem = addItem(trip.id(), firstDay.id(), "B");
        addItem(trip.id(), secondDay.id(), "C");
        addItem(trip.id(), secondDay.id(), "D");

        tripService.moveTripItem(userId, trip.id(), movingItem.id(),
                new MoveTripItemRequest(secondDay.id(), 0));

        TripDetailResponse result = tripService.getTripDetail(userId, trip.id());
        assertItems(result.days().get(0).items(), List.of("A"));
        assertItems(result.days().get(1).items(), List.of("B", "C", "D"));
    }

    private TripDetailResponse createTripWithDays(int dayCount) {
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        UUID tripId = tripService.createTrip(userId,
                new CreateTripRequest("테스트 여행", startDate,
                        startDate.plusDays(dayCount - 1))).id();
        return tripService.getTripDetail(userId, tripId);
    }

    private TripItemResponse addItem(UUID tripId, UUID dayId, String contentId) {
        CartItem cartItem = saveCartItem(user, contentId, contentId, null);
        return tripService.addTripItem(userId, tripId, dayId,
                new AddTripItemRequest(cartItem.getId(), null, null));
    }

    private CartItem saveCartItem(User owner, String contentId, String title, String imageUrl) {
        return cartItemRepository.save(new CartItem(owner, contentId, title, imageUrl, "1"));
    }

    private void assertItems(List<TripItemResponse> items, List<String> expectedIds) {
        assertEquals(expectedIds, items.stream().map(TripItemResponse::tourApiContentId).toList());
        assertEquals(
                java.util.stream.IntStream.range(0, items.size()).boxed().toList(),
                items.stream().map(TripItemResponse::orderIndex).toList()
        );
    }
}
