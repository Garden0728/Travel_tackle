package Timeout.travel_tackle.config;

import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.TripDay;
import Timeout.travel_tackle.entity.TripItem;
import Timeout.travel_tackle.entity.TripPhoto;
import Timeout.travel_tackle.entity.TripRecord;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.tour.dto.TourDtos.ContentSummary;
import Timeout.travel_tackle.tour.service.TourService;
import Timeout.travel_tackle.trip.repository.TripDayRepository;
import Timeout.travel_tackle.trip.repository.TripItemRepository;
import Timeout.travel_tackle.trip.repository.TripPhotoRepository;
import Timeout.travel_tackle.trip.repository.TripRecordRepository;
import Timeout.travel_tackle.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 실사용자가 없는 지금 단계에서 여행자 피드(/api/feed)를 눈으로 확인/테스트하기 위한 가상 데이터.
 * 하루당 5곳 이상의 장소를 TourAPI(TourService)에서 지역별로 실제 콘텐츠를 가져와 채우고,
 * TourAPI 호출이 실패하면(키 미설정, 네트워크 오류 등) 지역별 폴백 장소로 대체한다.
 * ddl-auto: create라 매 부팅시 DB가 초기화되므로 이 시더도 매번 다시 실행된다.
 * 실사용자가 생기면 app.seed.enabled=false로 끄면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", matchIfMissing = true)
public class DemoFeedSeeder implements CommandLineRunner {

    private record Stop(String placeName, String address) {
    }

    private record PlaceData(String contentId, String title, String address, String imageUrl) {
    }

    private static final String ATTRACTION_CONTENT_TYPE_ID = "12"; // 관광지
    private static final int PLACES_PER_DAY = 5;
    private static final LocalTime[] SLOT_START_TIMES =
            {LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(13, 0), LocalTime.of(15, 0), LocalTime.of(17, 0)};

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final TripItemRepository tripItemRepository;
    private final TripRecordRepository tripRecordRepository;
    private final TripPhotoRepository tripPhotoRepository;
    private final TourService tourService;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("demo.haneul@travel-tackle.local")
                .map(u -> !tripRepository.findAllByUserOrderByCreatedAtDesc(u).isEmpty())
                .orElse(false)) {
            log.info("여행자 피드 시드 데이터가 이미 있어 다시 생성하지 않습니다.");
            return;
        }

        User seoulUser = user("demo.haneul@travel-tackle.local", "하늘");
        User busanUser = user("demo.bada@travel-tackle.local", "바다");
        User jejuUser = user("demo.gureum@travel-tackle.local", "구름");
        User gangneungUser = user("demo.byeol@travel-tackle.local", "별");
        User suwonUser = user("demo.noeul@travel-tackle.local", "노을");

        seedPlanOnly(seoulUser, "서울 2박 3일 나들이", "1", 3, List.of(
                new Stop("경복궁", "서울특별시 종로구 사직로 161"),
                new Stop("남산타워", "서울특별시 용산구 남산공원길 105"),
                new Stop("홍대 거리", "서울특별시 마포구 양화로 45"),
                new Stop("광장시장", "서울특별시 종로구 창경궁로 88"),
                new Stop("이태원 거리", "서울특별시 용산구 이태원로 177")));

        seedPlanWithRecord(busanUser, "부산 1박 2일 바다 여행", "6", 2, List.of(
                new Stop("해운대 해수욕장", "부산광역시 해운대구 해운대해변로 264"),
                new Stop("광복로 거리", "부산광역시 중구 중앙대로 22"),
                new Stop("감천문화마을", "부산광역시 사하구 감내2로 203"),
                new Stop("태종대", "부산광역시 영도구 전망로 99"),
                new Stop("자갈치시장", "부산광역시 중구 자갈치해안로 52")),
                "부산 바다가 최고였던 여행", "해운대 노을이 진짜 예뻤어요. 다음에 또 가고 싶은 곳!");

        seedPlanWithRecord(jejuUser, "제주 2박 3일 힐링 코스", "39", 3, List.of(
                new Stop("한라수목원", "제주특별자치도 제주시 수목원길 72"),
                new Stop("중문관광단지", "제주특별자치도 서귀포시 중문관광로 72"),
                new Stop("애월 해안도로", "제주특별자치도 제주시 애월읍 애월북서길 56"),
                new Stop("성산일출봉", "제주특별자치도 서귀포시 성산읍 성산리 1"),
                new Stop("우도", "제주특별자치도 제주시 우도면 우도해안길 32")),
                "제주에서 보낸 힐링 여행", "날씨도 좋고 음식도 맛있었던 제주 여행 기록입니다.");

        seedPlanOnly(gangneungUser, "강릉 1박 2일 커피 여행", "32", 2, List.of(
                new Stop("안목해변 카페거리", "강원특별자치도 강릉시 창해로 350"),
                new Stop("경포호", "강원특별자치도 강릉시 경포로 365"),
                new Stop("정동진", "강원특별자치도 강릉시 강동면 정동진리 산 17"),
                new Stop("오죽헌", "강원특별자치도 강릉시 율곡로3139번길 24"),
                new Stop("강릉 중앙시장", "강원특별자치도 강릉시 성남동 39")));

        seedPlanWithRecord(suwonUser, "수원 2박 3일 역사 탐방", "31", 3, List.of(
                new Stop("수원 화성", "경기도 수원시 팔달구 정조로 825"),
                new Stop("에버랜드", "경기도 용인시 처인구 포곡읍 에버랜드로 199"),
                new Stop("광교호수공원", "경기도 수원시 영통구 광교로 145"),
                new Stop("행궁동 벽화마을", "경기도 수원시 팔달구 신풍로 41"),
                new Stop("수원화성행궁", "경기도 수원시 팔달구 정조로 825")),
                "수원 화성 다녀온 후기", "수원 화성 성곽길 걷기 좋았고, 근처 맛집도 많았어요.");
    }

    private User user(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.socialUser(email, name)));
    }

    private void seedPlanOnly(User user, String title, String areaCode, int dayCount, List<Stop> fallback) {
        Trip trip = createTripWithItems(user, title, areaCode, dayCount, fallback);
        trip.publish();
        tripRepository.save(trip);
    }

    private void seedPlanWithRecord(User user, String title, String areaCode, int dayCount, List<Stop> fallback,
                                     String recordTitle, String recordContent) {
        Trip trip = createTripWithItems(user, title, areaCode, dayCount, fallback);
        trip.publish();
        trip.complete();
        tripRepository.save(trip);

        TripRecord record = tripRecordRepository.save(new TripRecord(trip, recordTitle, recordContent));
        tripPhotoRepository.saveAllAndFlush(List.of(
                new TripPhoto(record, "https://picsum.photos/seed/" + trip.getId() + "-1/800/600", null),
                new TripPhoto(record, "https://picsum.photos/seed/" + trip.getId() + "-2/800/600", null)
        ));
    }

    private Trip createTripWithItems(User user, String title, String areaCode, int dayCount, List<Stop> fallback) {
        int placesNeeded = dayCount * PLACES_PER_DAY;
        List<PlaceData> places = resolvePlaces(areaCode, placesNeeded, fallback);

        LocalDate start = LocalDate.now().minusDays(14);
        Trip trip = tripRepository.save(new Trip(user, title, start, start.plusDays(dayCount - 1)));

        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            TripDay day = tripDayRepository.save(new TripDay(trip, dayIndex + 1, start.plusDays(dayIndex)));
            for (int slot = 0; slot < PLACES_PER_DAY; slot++) {
                PlaceData place = places.get(dayIndex * PLACES_PER_DAY + slot);
                LocalTime startTime = SLOT_START_TIMES[slot];
                TripItem item = new TripItem(
                        day, place.contentId(), place.title(),
                        place.imageUrl(),
                        areaCode, ATTRACTION_CONTENT_TYPE_ID, null, null, null,
                        startTime, startTime.plusHours(1), slot,
                        place.address(), "직접 남긴 메모예요.");
                tripItemRepository.save(item);
            }
        }
        return trip;
    }

    /** TourAPI에서 지역별 관광지를 필요한 개수만큼 가져오고, 실패하거나 부족하면 폴백 장소로 채운다. */
    private List<PlaceData> resolvePlaces(String areaCode, int placesNeeded, List<Stop> fallback) {
        List<PlaceData> real = fetchRealPlaces(areaCode, placesNeeded);
        List<PlaceData> places = new ArrayList<>(real);
        for (int i = places.size(); i < placesNeeded; i++) {
            Stop stop = fallback.get(i % fallback.size());
            places.add(new PlaceData("seed-fallback-" + areaCode + "-" + i, stop.placeName(), stop.address(), null));
        }
        return places.stream()
                .map(place -> place.imageUrl() != null ? place
                        : new PlaceData(place.contentId(), place.title(), place.address(),
                                "https://picsum.photos/seed/" + place.contentId() + "/400/500"))
                .toList();
    }

    private List<PlaceData> fetchRealPlaces(String areaCode, int count) {
        try {
            List<ContentSummary> contents = tourService
                    .getContents(null, areaCode, null, ATTRACTION_CONTENT_TYPE_ID, 1, count, null)
                    .items();
            return contents.stream()
                    .filter(c -> StringUtils.hasText(c.title()) && StringUtils.hasText(c.address()))
                    .map(c -> new PlaceData(c.contentId(), c.title(), c.address(), c.imageUrl()))
                    .toList();
        } catch (Exception e) {
            log.warn("TourAPI 호출 실패 (areaCode={}) - 폴백 장소로 대체합니다.", areaCode, e);
            return List.of();
        }
    }
}
