package Timeout.travel_tackle.auth.service;

import Timeout.travel_tackle.auth.dto.SignupRequest;
import Timeout.travel_tackle.auth.dto.SignupResponse;
import Timeout.travel_tackle.auth.repository.UserAuthProviderRepository;
import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.entity.EmailVerification;
import Timeout.travel_tackle.entity.Enum.AuthProvider;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.entity.UserAuthProvider;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = EmailVerificationService.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = emailVerificationService.getVerifiedAndUsable(email, now);
        String nationality = normalizeNationality(request.nationality());

        User user = User.localUser(
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                nationality
        );
        userRepository.save(user);
        userAuthProviderRepository.save(new UserAuthProvider(user, AuthProvider.LOCAL, email));
        verification.use(now);

        return SignupResponse.from(user);
    }

    private String normalizeNationality(String nationality) {
        return nationality == null || nationality.isBlank()
                ? null
                : nationality.trim().toUpperCase();
    }
}
