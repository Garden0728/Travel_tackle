package Timeout.travel_tackle.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //Trip에 관련 예외
    INVALID_TRIP_DATE_RANGE(HttpStatus.BAD_REQUEST, "TRIP_001", "여행 종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_DAY_NUMBER(HttpStatus.BAD_REQUEST, "TRIP_002", "여행 일차는 1 이상이어야 합니다."),
    INVALID_TRIP_ITEM_TIME_RANGE(HttpStatus.BAD_REQUEST, "TRIP_003", "일정 종료 시간은 시작 시간보다 빠를 수 없습니다."),
    INVALID_ORDER_INDEX(HttpStatus.BAD_REQUEST, "TRIP_004", "일정 순서는 0 이상이어야 합니다."),

    //Auth에 관련 예외
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_001", "이미 가입된 이메일입니다."),
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTH_002", "이메일 인증 요청을 찾을 수 없습니다."),
    INVALID_EMAIL_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_003", "이메일 인증번호가 올바르지 않습니다."),
    EMAIL_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_004", "이메일 인증번호가 만료되었습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_005", "이메일 인증이 완료되지 않았습니다."),
    EMAIL_VERIFICATION_ALREADY_USED(HttpStatus.BAD_REQUEST, "AUTH_006", "이미 사용된 이메일 인증입니다."),
    EMAIL_VERIFICATION_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_007", "이메일 인증 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    EMAIL_DELIVERY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_008", "인증 이메일을 발송하지 못했습니다. 잠시 후 다시 시도해 주세요."),

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_002", "요청 값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
