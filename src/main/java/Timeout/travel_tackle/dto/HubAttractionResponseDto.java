package Timeout.travel_tackle.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HubAttractionResponseDto {
    private String hubTatsNm;

    private String hubCtgryLclsNm;

    private String hubCtgryMclsNm;

    private Integer hubRank;

    private Double mapX;

    private Double mapY;
}