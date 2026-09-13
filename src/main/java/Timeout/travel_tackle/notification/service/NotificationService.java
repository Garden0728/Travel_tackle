package Timeout.travel_tackle.notification.service;

import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.entity.Notification;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.notification.dto.FeedbackNotificationCommand;
import Timeout.travel_tackle.notification.dto.NotificationPageResponse;
import Timeout.travel_tackle.notification.dto.NotificationPushEvent;
import Timeout.travel_tackle.notification.dto.NotificationResponse;
import Timeout.travel_tackle.notification.dto.UnreadCountResponse;
import Timeout.travel_tackle.notification.repository.NotificationRepository;
import Timeout.travel_tackle.notification.sse.NotificationSseRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String EVENT_NOTIFICATION = "notification";
    public static final String EVENT_UNREAD_COUNT = "unread-count";
    private static final int PREVIEW_LENGTH = 60;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationSseRegistry sseRegistry;

    /**
     * 참견 알림 저장 + 커밋 후 SSE 푸시. 받는 사람이 참견 알림을 꺼뒀거나 없으면 아무것도 하지 않는다.
     * 호출자 트랜잭션에 참여하므로 참견과 알림은 함께 커밋되거나 함께 롤백된다.
     */
    @Transactional
    public void notifyFeedback(FeedbackNotificationCommand command) {
        if (command.receiverId().equals(command.actorId())) {
            return;
        }
        User receiver = userRepository.findById(command.receiverId()).orElse(null);
        if (receiver == null || !receiver.isNotifyFeedback()) {
            return;
        }
        // flush 해야 @CreationTimestamp 가 채워진 채로 푸시 페이로드를 만들 수 있다
        Notification saved = notificationRepository.saveAndFlush(Notification.feedback(
                receiver, command.actorId(), command.actorName(),
                command.tripId(), command.tripTitle(), command.thumbnailUrl(),
                command.feedbackId(), command.target(), command.dayNumber(), command.itemTitle(),
                preview(command.content())));
        NotificationResponse response = NotificationResponse.from(saved);
        UUID receiverId = receiver.getId();
        afterCommit(() -> sseRegistry.send(receiverId, EVENT_NOTIFICATION,
                new NotificationPushEvent(response, notificationRepository.countByUserIdAndReadFalse(receiverId))));
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(UUID userId, Pageable pageable) {
        return NotificationPageResponse.of(
                notificationRepository.countByUserIdAndReadFalse(userId),
                notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable).map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isRead()) {
            notification.markRead();
            pushUnreadCountAfterCommit(userId);
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        if (notificationRepository.markAllReadByUserId(userId) > 0) {
            pushUnreadCountAfterCommit(userId);
        }
    }

    // 다른 탭이 열려 있을 수 있으니 읽음 처리 뒤 종 아이콘 숫자도 밀어준다
    private void pushUnreadCountAfterCommit(UUID userId) {
        afterCommit(() -> sseRegistry.send(userId, EVENT_UNREAD_COUNT,
                new UnreadCountResponse(notificationRepository.countByUserIdAndReadFalse(userId))));
    }

    // 커밋 전에 보내면 프론트가 바로 목록을 조회했을 때 아직 안 보일 수 있다.
    // 푸시는 부가 기능이라 실패해도 이미 커밋된 요청을 오류로 만들지 않는다 (알림은 저장돼 있어 목록으로 볼 수 있다)
    private static void afterCommit(Runnable action) {
        Runnable safe = () -> {
            try {
                action.run();
            } catch (RuntimeException e) {
                log.warn("알림 푸시 실패 (저장은 완료됨)", e);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safe.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safe.run();
            }
        });
    }

    private static String preview(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.strip();
        return trimmed.length() <= PREVIEW_LENGTH ? trimmed : trimmed.substring(0, PREVIEW_LENGTH) + "…";
    }
}
