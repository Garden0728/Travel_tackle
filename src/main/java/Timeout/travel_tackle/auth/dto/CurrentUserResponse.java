package Timeout.travel_tackle.auth.dto;

import Timeout.travel_tackle.entity.User;

import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String email,
        String name,
        String nationality
) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNationality()
        );
    }
}
