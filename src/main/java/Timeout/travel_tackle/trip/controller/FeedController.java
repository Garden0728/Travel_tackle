package Timeout.travel_tackle.trip.controller;

import Timeout.travel_tackle.trip.dto.FeedItemResponse;
import Timeout.travel_tackle.trip.dto.FeedSort;
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
import org.springframework.util.StringUtils;

import java.util.UUID;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "공개 여행 피드 API")
public class FeedController {

    private static final int MAX_PAGE_SIZE = 50;

    private final FeedService feedService;

    @GetMapping
    @Operation(summary = "공개 여행 피드 조회 (페이지네이션, keyword로 계획/기록 검색, sort=latest|oldest|popular|relevance)")
    public ResponseEntity<Page<FeedItemResponse>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String keyword
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        boolean hasKeyword = StringUtils.hasText(keyword);
        FeedSort feedSort = FeedSort.from(sort);
        if (feedSort == FeedSort.RELEVANCE && !hasKeyword) {
            feedSort = FeedSort.LATEST; // 키워드 없이 relevance 요청 시 최신순으로 대체
        }

        Pageable pageable;
        if (hasKeyword) {
            pageable = PageRequest.of(safePage, safeSize); // 정렬은 QueryDSL 쿼리 안에서 처리
        } else if (feedSort == FeedSort.OLDEST) {
            pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "createdAt"));
        } else if (feedSort == FeedSort.LATEST) {
            pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            pageable = PageRequest.of(safePage, safeSize);
        }

        return ResponseEntity.ok(feedService.getFeed(pageable, feedSort, keyword));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "공개 여행 상세 조회 (일정 + 사진 + 작성자)")
    public ResponseEntity<PublicTripDetailResponse> getPublicTripDetail(@PathVariable UUID tripId) {
        return ResponseEntity.ok(feedService.getPublicTripDetail(tripId));
    }
}
