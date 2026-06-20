package Timeout.travel_tackle.auth.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {

    private static final int CODE_BOUND = 1_000_000;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return String.format("%06d", secureRandom.nextInt(CODE_BOUND));
    }
}
