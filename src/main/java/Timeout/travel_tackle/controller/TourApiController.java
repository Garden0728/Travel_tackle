package Timeout.travel_tackle.controller;

import Timeout.travel_tackle.dto.HubAttractionResponseDto;
import Timeout.travel_tackle.service.HubAttractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tour")
public class TourApiController {

    private final HubAttractionService hubAttractionService;

    @GetMapping("/hub")
    public List<HubAttractionResponseDto> getHub(
            @RequestParam(required = false) String areaCd,
            @RequestParam(required = false) String signguCd
    ) {
        return hubAttractionService.getHubAttractions(areaCd, signguCd);
    }
}