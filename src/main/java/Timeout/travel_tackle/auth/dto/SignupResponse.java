package Timeout.travel_tackle.auth.dto;

import Timeout.travel_tackle.entity.User;

import java.util.UUID;

public record SignupResponse(
        UUID userId,
        String email,
        String name
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getName());
    }
}
