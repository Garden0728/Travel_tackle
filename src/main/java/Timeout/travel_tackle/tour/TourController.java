package Timeout.travel_tackle.tour;

import Timeout.travel_tackle.tour.TourDtos.Area;
import Timeout.travel_tackle.tour.TourDtos.Category;
import Timeout.travel_tackle.tour.TourDtos.ContentDetail;
import Timeout.travel_tackle.tour.TourDtos.ContentSummary;
import Timeout.travel_tackle.tour.TourDtos.Festival;
import Timeout.travel_tackle.tour.TourDtos.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/tour")
@RequiredArgsConstructor
@Tag(name = "Tour", description = "한국관광공사 관광정보 API")
public class TourController {

    private final TourService tourService;

    @GetMapping("/areas")
    @Operation(summary = "지역 또는 시군구 코드 조회")
    public List<Area> getAreas(@RequestParam(required = false) String areaCode) {
        return tourService.getAreas(areaCode);
    }

    @GetMapping("/categories")
    @Operation(summary = "관광 콘텐츠 분류코드 조회")
    public List<Category> getCategories(
            @RequestParam(required = false) String contentTypeId,
            @RequestParam(required = false) String category1,
            @RequestParam(required = false) String category2,
            @RequestParam(required = false) String category3
    ) {
        return tourService.getCategories(contentTypeId, category1, category2, category3);
    }

    @GetMapping("/contents")
    @Operation(summary = "지역별 관광 콘텐츠 조회 및 키워드 검색")
    public Page<ContentSummary> getContents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String sigunguCode,
            @RequestParam(required = false) String contentTypeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "A") String arrange
    ) {
        return tourService.getContents(
                keyword, areaCode, sigunguCode, contentTypeId, page, size, arrange);
    }

    @GetMapping("/contents/nearby")
    @Operation(summary = "좌표 기반 주변 관광 콘텐츠 조회")
    public Page<ContentSummary> getNearbyContents(
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "5000") int radius,
            @RequestParam(required = false) String contentTypeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return tourService.getNearbyContents(
                longitude, latitude, radius, contentTypeId, page, size);
    }

    @GetMapping("/contents/{contentId}")
    @Operation(summary = "관광 콘텐츠 상세 및 이미지 조회")
    public ContentDetail getContentDetail(@PathVariable String contentId) {
        return tourService.getContentDetail(contentId);
    }

    @GetMapping("/festivals")
    @Operation(summary = "기간별 축제·행사 조회")
    public Page<Festival> getFestivals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String areaCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return tourService.getFestivals(startDate, endDate, areaCode, page, size);
    }

    @GetMapping("/stays")
    @Operation(summary = "지역별 숙박 조회")
    public Page<ContentSummary> getStays(
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String sigunguCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return tourService.getStays(areaCode, sigunguCode, page, size);
    }
}
