package Timeout.travel_tackle.auth.service;

import Timeout.travel_tackle.auth.dto.CurrentUserResponse;
import Timeout.travel_tackle.auth.dto.LoginRequest;
import Timeout.travel_tackle.auth.jwt.AuthCookieService;
import Timeout.travel_tackle.auth.jwt.RefreshTokenService;
import Timeout.travel_tackle.auth.jwt.RefreshTokenService.AuthTokens;
import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;
    private final EmailNormalizer emailNormalizer;

    @Transactional
    public CurrentUserResponse login(LoginRequest request, HttpServletResponse response) {
        String email = emailNormalizer.normalize(request.email());
        User user = userRepository.findByEmail(email)
                .filter(candidate -> candidate.getPasswordHash() != null)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        AuthTokens tokens = refreshTokenService.issueTokens(user);
        authCookieService.writeTokens(response, tokens);
        return CurrentUserResponse.from(user);
    }

    @Transactional
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authCookieService.readRefreshToken(request);
        AuthTokens tokens = refreshTokenService.refreshTokens(refreshToken);
        authCookieService.writeTokens(response, tokens);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokenService.revokeRefreshToken(authCookieService.readRefreshToken(request));
        authCookieService.clearTokens(response);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(String subject) {
        try {
            UUID userId = UUID.fromString(subject);
            return userRepository.findById(userId)
                    .map(CurrentUserResponse::from)
                    .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
