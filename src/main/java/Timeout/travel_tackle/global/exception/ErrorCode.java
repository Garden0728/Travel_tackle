package Timeout.travel_tackle.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_TRIP_DATE_RANGE(HttpStatus.BAD_REQUEST, "TRIP_001", "여행 종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_DAY_NUMBER(HttpStatus.BAD_REQUEST, "TRIP_002", "여행 일차는 1 이상이어야 합니다."),
    INVALID_TRIP_ITEM_TIME_RANGE(HttpStatus.BAD_REQUEST, "TRIP_003", "일정 종료 시간은 시작 시간보다 빠를 수 없습니다."),
    INVALID_ORDER_INDEX(HttpStatus.BAD_REQUEST, "TRIP_004", "일정 순서는 0 이상이어야 합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
