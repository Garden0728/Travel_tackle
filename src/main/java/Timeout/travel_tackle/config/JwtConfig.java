package Timeout.travel_tackle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtProperties jwtProperties(
            @Value("${JWT_SECRET}") String secret,
            @Value("${JWT_ACCESS_TOKEN_SECONDS:900}") long accessTokenSeconds,
            @Value("${JWT_REFRESH_TOKEN_DAYS:14}") long refreshTokenDays,
            @Value("${AUTH_COOKIE_SECURE:false}") boolean secureCookie
    ) {
        return new JwtProperties(
                secret,
                Duration.ofSeconds(accessTokenSeconds),
                Duration.ofDays(refreshTokenDays),
                secureCookie,
                "travel-tackle"
        );
    }

    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey secretKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    public record JwtProperties(
            String secret,
            Duration accessTokenTtl, //만료시간
            Duration refreshTokenTtl, //만료 시간
            boolean secureCookie,
            String issuer
    ) {
        public JwtProperties {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
            }
        }
    }
}
