package Timeout.travel_tackle.trip.controller;

import Timeout.travel_tackle.trip.dto.FeedItemResponse;
import Timeout.travel_tackle.trip.dto.PublicTripDetailResponse;
import Timeout.travel_tackle.trip.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "공개 여행 피드 API")
public class FeedController {

    private static final int MAX_PAGE_SIZE = 50;

    private final FeedService feedService;

    @GetMapping
    @Operation(summary = "공개 여행 피드 조회 (최신순 페이지네이션)")
    public ResponseEntity<Page<FeedItemResponse>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(feedService.getFeed(pageable));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "공개 여행 상세 조회 (일정 + 사진 + 작성자)")
    public ResponseEntity<PublicTripDetailResponse> getPublicTripDetail(@PathVariable UUID tripId) {
        return ResponseEntity.ok(feedService.getPublicTripDetail(tripId));
    }
}
