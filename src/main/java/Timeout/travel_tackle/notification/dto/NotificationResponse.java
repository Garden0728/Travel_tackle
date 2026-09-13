package Timeout.travel_tackle.notification.dto;

import Timeout.travel_tackle.entity.Enum.NotificationTarget;
import Timeout.travel_tackle.entity.Enum.NotificationType;
import Timeout.travel_tackle.entity.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

/** 알림 하나로 "누가 · 내 어떤 계획에 · 어디에 · 무슨 말을" 남겼는지 알 수 있게 구성한다. */
public record NotificationResponse(
        UUID id,
        NotificationType type,
        boolean read,
        LocalDateTime createdAt,
        Actor actor,
        TripSummary trip,
        FeedbackSummary feedback
) {
    public record Actor(UUID id, String name) {
    }

    public record TripSummary(UUID id, String title, String thumbnailUrl) {
    }

    public record FeedbackSummary(UUID id, NotificationTarget target, Integer dayNumber, String itemTitle, String preview) {
    }

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.isRead(),
                n.getCreatedAt(),
                n.getActorId() == null && n.getActorName() == null ? null : new Actor(n.getActorId(), n.getActorName()),
                n.getTripId() == null ? null : new TripSummary(n.getTripId(), n.getTripTitle(), n.getThumbnailUrl()),
                n.getFeedbackId() == null ? null
                        : new FeedbackSummary(n.getFeedbackId(), n.getTarget(), n.getDayNumber(), n.getItemTitle(), n.getPreview())
        );
    }
}
