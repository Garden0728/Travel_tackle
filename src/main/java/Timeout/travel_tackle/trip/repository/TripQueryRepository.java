package Timeout.travel_tackle.trip.repository;

import Timeout.travel_tackle.entity.QTripDay;
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
     * 여행 전체 삭제 — 서브쿼리로 TripItem, TripDay 각 1번의 DELETE로 처리
     */
    public void bulkDeleteByTrip(Trip trip) {
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
}
