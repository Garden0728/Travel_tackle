package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.QTripDay;
import Timeout.travel_tackle.entity.QTripFeedback;
import Timeout.travel_tackle.entity.QTripFeedbackRecommendation;
import Timeout.travel_tackle.entity.QTripItem;
import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.TripDay;
import Timeout.travel_tackle.entity.TripItem;
import Timeout.travel_tackle.trip.dto.TripDayResponse;
import Timeout.travel_tackle.trip.dto.TripDetailResponse;
import Timeout.travel_tackle.trip.dto.TripItemResponse;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TripQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 여행 상세 조회 — N+1 없이 쿼리 2번으로 전체 일정 로딩
     *   쿼리 1: TripDay 목록 (아이템 없는 날도 포함)
     *   쿼리 2: TripItem 전체 + TripDay 페치조인 (1번에 로딩)
     */
    public TripDetailResponse findDetail(Trip trip) {
        QTripDay qDay = new QTripDay("qDay");
        QTripItem qItem = QTripItem.tripItem;

        List<TripDay> days = queryFactory
                .selectFrom(qDay)
                .where(qDay.trip.eq(trip))
                .orderBy(qDay.dayNumber.asc())
                .fetch();

        List<TripItem> items = queryFactory
                .selectFrom(qItem)
                .join(qItem.tripDay, qDay).fetchJoin()
                .where(qDay.trip.eq(trip))
                .orderBy(qDay.dayNumber.asc(), qItem.orderIndex.asc())
                .fetch();

        Map<UUID, List<TripItem>> itemsByDayId = items.stream()
                .collect(Collectors.groupingBy(i -> i.getTripDay().getId()));

        List<TripDayResponse> dayResponses = days.stream()
                .map(day -> TripDayResponse.of(day,
                        itemsByDayId.getOrDefault(day.getId(), List.of()).stream()
                                .map(TripItemResponse::from).toList()))
                .toList();

        return TripDetailResponse.of(trip, dayResponses);
    }

    /**
     * 여행 전체 삭제 — deleteTrip()에서 호출.
     * 피드백 추천 → 피드백 → 아이템 → 일차 순으로 삭제 (FK 제약 준수)
     */
    public void bulkDeleteByTrip(Trip trip) {
        QTripDay qDay = new QTripDay("qDay");
        QTripItem qItem = QTripItem.tripItem;
        QTripFeedback qFeedback = QTripFeedback.tripFeedback;
        QTripFeedbackRecommendation qRec = QTripFeedbackRecommendation.tripFeedbackRecommendation;

        queryFactory.delete(qRec)
                .where(qRec.feedback.in(
                        JPAExpressions.selectFrom(qFeedback).where(qFeedback.trip.eq(trip))
                ))
                .execute();

        queryFactory.delete(qFeedback)
                .where(qFeedback.trip.eq(trip))
                .execute();

        queryFactory.delete(qItem)
                .where(qItem.tripDay.in(
                        JPAExpressions.selectFrom(qDay).where(qDay.trip.eq(trip))
                ))
                .execute();

        queryFactory.delete(qDay)
                .where(qDay.trip.eq(trip))
                .execute();
    }

    /**
     * 날짜 변경 시 일차·아이템만 삭제. 피드백은 건드리지 않는다.
     * (피드백 day/item 참조는 bulkNullifyFeedbackReferences로 먼저 끊어둔다)
     */
    public void bulkDeleteDaysAndItems(Trip trip) {
        QTripDay qDay = new QTripDay("qDay");
        QTripItem qItem = QTripItem.tripItem;

        queryFactory.delete(qItem)
                .where(qItem.tripDay.in(
                        JPAExpressions.selectFrom(qDay).where(qDay.trip.eq(trip))
                ))
                .execute();

        queryFactory.delete(qDay)
                .where(qDay.trip.eq(trip))
                .execute();
    }

    /**
     * 날짜 변경으로 일차·아이템을 초기화할 때 피드백의 day/item 참조를 null로 처리.
     * 피드백 텍스트는 보존하고 참조만 끊는다.
     */
    public void bulkNullifyFeedbackReferences(Trip trip) {
        QTripFeedback qFeedback = QTripFeedback.tripFeedback;
        QTripDay qDay = new QTripDay("qDay");

        queryFactory.update(qFeedback)
                .setNull(qFeedback.tripItem)
                .where(qFeedback.trip.eq(trip), qFeedback.tripItem.isNotNull())
                .execute();

        queryFactory.update(qFeedback)
                .setNull(qFeedback.tripDay)
                .where(qFeedback.trip.eq(trip), qFeedback.tripDay.isNotNull())
                .execute();
    }
}
