package Timeout.travel_tackle.auth.controller;

import Timeout.travel_tackle.auth.dto.EmailVerificationConfirmRequest;
import Timeout.travel_tackle.auth.dto.EmailVerificationRequest;
import Timeout.travel_tackle.auth.dto.SignupRequest;
import Timeout.travel_tackle.auth.dto.SignupResponse;
import Timeout.travel_tackle.auth.service.EmailVerificationService;
import Timeout.travel_tackle.auth.service.SignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "이메일 인증 및 일반 회원가입 API")
@SecurityRequirements
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final SignupService signupService;

    @PostMapping("/email-verifications")
    @Operation(summary = "이메일 인증번호 요청")
    public ResponseEntity<Void> requestEmailVerification(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        emailVerificationService.requestCode(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/email-verifications/confirm")
    @Operation(summary = "이메일 인증번호 확인")
    public ResponseEntity<Void> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        emailVerificationService.confirmCode(request.email(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signup")
    @Operation(summary = "일반 회원가입")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(signupService.signup(request));
    }
}
