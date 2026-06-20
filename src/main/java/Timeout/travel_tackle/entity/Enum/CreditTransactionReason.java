package Timeout.travel_tackle.entity.Enum;

public enum CreditTransactionReason {
    TRIP_SAVE, // 다른 사용자의 여행 계획을 저장할 때 크레딧 사용
    CHARGE,    // 결제 등을 통해 크레딧 충전
    REWARD,    // 이벤트나 서비스 활동 보상으로 크레딧 지급
    REFUND     // 취소 또는 오류 처리로 사용한 크레딧 환급
}
