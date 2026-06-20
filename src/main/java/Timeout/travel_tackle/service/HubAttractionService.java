package Timeout.travel_tackle.service;

import Timeout.travel_tackle.dto.HubAttractionApiResponseDto;
import Timeout.travel_tackle.dto.HubAttractionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HubAttractionService {

    //기본 지역코드(사용자 미입력 시 사용)
    private static final String DEFAULT_AREA_CD = "11"; //서울
    private static final String DEFAULT_SIGNGU_CD = "11110"; //종로구

    @Value("${tour.service-key}")
    private String serviceKey;

    private final RestClient restClient =
            RestClient.create();

    public List<HubAttractionResponseDto> getHubAttractions(String areaCd, String signguCd) {

        String newAreaCd = StringUtils.hasText(areaCd) ? areaCd : DEFAULT_AREA_CD;
        String newSignguCd = StringUtils.hasText(signguCd) ? signguCd : DEFAULT_SIGNGU_CD;

        HubAttractionApiResponseDto response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("apis.data.go.kr")
                        .path("/B551011/LocgoHubTarService1/areaBasedList1")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 10)
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", "Travel-tackle")
                        .queryParam("baseYm", "202604")
                        .queryParam("areaCd", newAreaCd)
                        .queryParam("signguCd", newSignguCd)
                        .queryParam("_type", "json")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("공공데이터 API 호출 실패: " + res.getStatusCode());
                })
                .body(HubAttractionApiResponseDto.class);

        if (response == null
                || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null) {
            return List.of();
        }

        return response.getResponse().getBody().getItems().getItem();
    }
}