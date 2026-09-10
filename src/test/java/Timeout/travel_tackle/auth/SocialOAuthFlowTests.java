package Timeout.travel_tackle.auth;

import Timeout.travel_tackle.auth.repository.UserAuthProviderRepository;
import Timeout.travel_tackle.auth.social.service.SocialLoginService;
import Timeout.travel_tackle.entity.Enum.AuthProvider;
import Timeout.travel_tackle.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "social.login.enabled=true",
        "KAKAO_CLIENT_ID=test-kakao-client",
        "KAKAO_CLIENT_SECRET=test-kakao-secret"
})
@AutoConfigureMockMvc
@Transactional
class SocialOAuthFlowTests {

    @Autowired MockMvc mockMvc;
    @Autowired ClientRegistrationRepository clientRegistrationRepository;
    @Autowired SocialLoginService socialLoginService;
    @Autowired UserAuthProviderRepository userAuthProviderRepository;
    @Autowired EntityManager entityManager;

    @Test
    void kakaoAuthorizationStartsWithoutHttpSession() throws Exception {
        assertNotNull(clientRegistrationRepository.findByRegistrationId("kakao"));

        MvcResult result = mockMvc.perform(get("/oauth2/authorization/kakao"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertTrue(result.getResponse().getRedirectedUrl().startsWith("https://kauth.kakao.com/oauth/authorize"));
        assertFalse(result.getResponse().getRedirectedUrl().contains("account_email"));
        Cookie authorizationCookie = result.getResponse().getCookie("oauth2_authorization_request");
        assertNotNull(authorizationCookie);
        assertTrue(authorizationCookie.isHttpOnly());
        assertNull(result.getResponse().getCookie("JSESSIONID"));
    }

    @Test
    void unauthenticatedApiRequestReturns401JsonInsteadOfLoginRedirect() throws Exception {
        // axios 기본 Accept 헤더 — 이 조합이 oauth2Login 의 로그인 리다이렉트 매처에 걸려 302 가 났었다
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.ACCEPT, "application/json, text/plain, */*"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("AUTH_013"));

        mockMvc.perform(get("/api/trips").header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void kakaoProfileCreatesUserWithoutEmail() {
        Map<String, Object> attributes = Map.of(
                "id", 123456789L,
                "properties", Map.of("nickname", "카카오 사용자"),
                "kakao_account", Map.of(
                        "profile", Map.of("nickname", "카카오 사용자")
                )
        );
        User user = socialLoginService.login(AuthProvider.KAKAO, attributes);

        assertNull(user.getEmail());
        assertEquals("카카오 사용자", user.getName());
        assertTrue(userAuthProviderRepository
                .findByProviderAndProviderUserId(AuthProvider.KAKAO, "123456789")
                .isPresent());

        entityManager.flush();
        entityManager.clear();
        User existingUser = socialLoginService.login(AuthProvider.KAKAO, attributes);
        assertTrue(Hibernate.isInitialized(existingUser));
    }

    @Test
    void googleProfileCreatesUserAndProviderAccount() {
        User user = socialLoginService.login(AuthProvider.GOOGLE, Map.of(
                "sub", "google-user-1",
                "email", "SOCIAL@example.com",
                "name", "소셜 사용자",
                "email_verified", true
        ));

        assertEquals("social@example.com", user.getEmail());
        assertNotNull(user.getEmailVerifiedAt());
        assertTrue(userAuthProviderRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-user-1")
                .isPresent());
    }
}
