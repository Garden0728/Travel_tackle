package Timeout.travel_tackle.trip.service;

import Timeout.travel_tackle.entity.Trip;
import Timeout.travel_tackle.entity.TripPhoto;
import Timeout.travel_tackle.entity.TripRecord;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.trip.dto.TripRecordRequest;
import Timeout.travel_tackle.trip.dto.TripRecordResponse;
import Timeout.travel_tackle.trip.repository.TripPhotoRepository;
import Timeout.travel_tackle.trip.repository.TripRecordRepository;
import Timeout.travel_tackle.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 여행 기록(후기). 계획당 1개, 내용 필수, 사진 1장 이상.
 */
@Service
@RequiredArgsConstructor
public class TripRecordService {

    private final TripRepository tripRepository;
    private final TripRecordRepository tripRecordRepository;
    private final TripPhotoRepository tripPhotoRepository;

    @Transactional
    public TripRecordResponse createRecord(UUID userId, UUID tripId, TripRecordRequest request) {
        Trip trip = findTripOwnedBy(userId, tripId);
        if (tripRecordRepository.existsByTrip(trip)) {
            throw new CustomException(ErrorCode.TRIP_RECORD_ALREADY_EXISTS);
        }
        TripRecord record = tripRecordRepository.save(new TripRecord(trip, request.content()));
        savePhotos(record, request);
        return buildResponse(record);
    }

    @Transactional(readOnly = true)
    public TripRecordResponse getRecord(UUID userId, UUID tripId) {
        Trip trip = findTripOwnedBy(userId, tripId);
        TripRecord record = findRecordOf(trip);
        return buildResponse(record);
    }

    @Transactional
    public TripRecordResponse updateRecord(UUID userId, UUID tripId, TripRecordRequest request) {
        Trip trip = findTripOwnedBy(userId, tripId);
        TripRecord record = findRecordOf(trip);
        record.updateContent(request.content());
        // 사진은 전체 교체
        tripPhotoRepository.deleteAllByRecord(record);
        tripPhotoRepository.flush();
        savePhotos(record, request);
        return buildResponse(record);
    }

    @Transactional
    public void deleteRecord(UUID userId, UUID tripId) {
        Trip trip = findTripOwnedBy(userId, tripId);
        TripRecord record = findRecordOf(trip);
        // 사진(자식) → 기록(부모) 순으로 제거해야 FK 제약 위반이 없다
        tripPhotoRepository.deleteAllByRecord(record);
        tripPhotoRepository.flush();
        tripRecordRepository.delete(record);
    }

    private void savePhotos(TripRecord record, TripRecordRequest request) {
        List<TripPhoto> photos = request.photos().stream()
                .map(entry -> new TripPhoto(record, entry.imageUrl(), entry.caption()))
                .toList();
        // flush 해야 @CreationTimestamp(uploadedAt)가 채워진 상태로 응답할 수 있다
        tripPhotoRepository.saveAllAndFlush(photos);
    }

    private TripRecordResponse buildResponse(TripRecord record) {
        return TripRecordResponse.of(record,
                tripPhotoRepository.findAllByRecordOrderByUploadedAtAsc(record));
    }

    private TripRecord findRecordOf(Trip trip) {
        return tripRecordRepository.findByTrip(trip)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_RECORD_NOT_FOUND));
    }

    private Trip findTripOwnedBy(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));
        if (!trip.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.TRIP_ACCESS_DENIED);
        }
        return trip;
    }
}
