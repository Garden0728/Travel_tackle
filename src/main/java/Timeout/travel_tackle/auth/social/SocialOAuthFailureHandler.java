package Timeout.travel_tackle.auth.social;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class SocialOAuthFailureHandler implements AuthenticationFailureHandler {

    private final String failureRedirectUrl;

    public SocialOAuthFailureHandler(
            @Value("${OAUTH_FAILURE_REDIRECT_URL:http://localhost:5173/login}")
            String failureRedirectUrl
    ) {
        this.failureRedirectUrl = failureRedirectUrl;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        log.warn("Social login failed: {}", exception.getClass().getSimpleName());
        String redirectUrl = UriComponentsBuilder.fromUriString(failureRedirectUrl)
                .queryParam("error", "social_login_failed")
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}
