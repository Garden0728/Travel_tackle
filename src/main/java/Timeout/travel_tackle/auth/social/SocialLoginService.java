package Timeout.travel_tackle.auth.social;

import Timeout.travel_tackle.auth.repository.UserAuthProviderRepository;
import Timeout.travel_tackle.auth.repository.UserRepository;
import Timeout.travel_tackle.auth.service.EmailNormalizer;
import Timeout.travel_tackle.entity.Enum.AuthProvider;
import Timeout.travel_tackle.entity.User;
import Timeout.travel_tackle.entity.UserAuthProvider;
import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final EmailNormalizer emailNormalizer;

    @Transactional
    public User login(AuthProvider provider, Map<String, Object> attributes) {
        SocialProfile profile = extractProfile(provider, attributes);
        validateProfile(provider, profile);

        return userAuthProviderRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId())
                .map(UserAuthProvider::getUser)
                .orElseGet(() -> createSocialUser(provider, profile));
    }

    private User createSocialUser(AuthProvider provider, SocialProfile profile) {
        String email = provider == AuthProvider.KAKAO
                ? null
                : emailNormalizer.normalize(profile.email());
        if (email != null && userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
        }

        String name = StringUtils.hasText(profile.name())
                ? profile.name().trim()
                : provider.name() + " 사용자";
        User user = User.socialUser(email, name);
        userRepository.save(user);
        userAuthProviderRepository.save(new UserAuthProvider(user, provider, profile.providerUserId()));
        return user;
    }

    private void validateProfile(AuthProvider provider, SocialProfile profile) {
        if (!StringUtils.hasText(profile.providerUserId())) {
            throw new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        if (provider == AuthProvider.KAKAO) {
            return;
        }
        if (!StringUtils.hasText(profile.email())) {
            throw new CustomException(ErrorCode.SOCIAL_EMAIL_REQUIRED);
        }
        if (!profile.emailVerified()) {
            throw new CustomException(ErrorCode.SOCIAL_EMAIL_NOT_VERIFIED);
        }
    }

    private SocialProfile extractProfile(AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case KAKAO -> extractKakaoProfile(attributes);
            case GOOGLE -> extractGoogleProfile(attributes);
            default -> throw new CustomException(ErrorCode.UNSUPPORTED_AUTH_PROVIDER);
        };
    }

    private SocialProfile extractKakaoProfile(Map<String, Object> attributes) {
        Map<String, Object> account = nestedMap(attributes, "kakao_account");
        Map<String, Object> profile = nestedMap(account, "profile");
        return new SocialProfile(
                stringValue(attributes.get("id")),
                null,
                stringValue(profile.get("nickname")),
                false
        );
    }

    private SocialProfile extractGoogleProfile(Map<String, Object> attributes) {
        return new SocialProfile(
                stringValue(attributes.get("sub")),
                stringValue(attributes.get("email")),
                stringValue(attributes.get("name")),
                Boolean.TRUE.equals(attributes.get("email_verified"))
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record SocialProfile(
            String providerUserId,
            String email,
            String name,
            boolean emailVerified
    ) {
    }
}
