package Timeout.travel_tackle.trip.dto;

/**
 * 피드 카드 종류. 한 Trip은 PLAN 카드 1개(항상)와, 기록(TripRecord)이 있으면 RECORD 카드 1개를
 * 별도 항목으로 낸다.
 */
public enum FeedItemType {
    PLAN,
    RECORD
}
