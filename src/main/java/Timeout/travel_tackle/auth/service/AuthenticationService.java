package Timeout.travel_tackle.auth.service;

import Timeout.travel_tackle.auth.dto.ChangePasswordRequest;
import Timeout.travel_tackle.auth.dto.CurrentUserResponse;
import Timeout.travel_tackle.auth.dto.LoginRequest;
import Timeout.travel_tackle.auth.jwt.AuthCookieService;
import Timeout.travel_tackle.auth.jwt.RefreshTokenService;
import Timeout.travel_tackle.auth.jwt.RefreshTokenService.AuthTokens;
import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import Timeout.travel_tackle.global.util.UuidConverter;
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
        UUID userId = UuidConverter.fromSubject(subject);
        return userRepository.findById(userId)
                .map(CurrentUserResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
    }

    @Transactional
    public void changePassword(String subject, ChangePasswordRequest request, HttpServletResponse response) {
        UUID userId = UuidConverter.fromSubject(subject);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));

        if (user.getPasswordHash() == null) {
            throw new CustomException(ErrorCode.NO_LOCAL_PASSWORD);
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
        // currentPassword는 위에서 이미 해시와 일치함이 확인됐으므로, newPassword가 그것과
        // 문자열까지 동일하면 굳이 BCrypt를 한 번 더 돌리지 않고 바로 판단할 수 있다.
        if (request.newPassword().equals(request.currentPassword())
                || passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAllTokens(user);

        // revokeAllTokens()는 clearAutomatically=true라 user가 detach된다.
        // detach된 엔티티로 issueTokens()를 호출하면 새 RefreshToken의 @ManyToOne 참조가
        // 깨질 수 있으니, 반드시 다시 조회한 뒤에 토큰을 발급한다.
        User reloaded = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
        AuthTokens tokens = refreshTokenService.issueTokens(reloaded);
        authCookieService.writeTokens(response, tokens);
    }
}
