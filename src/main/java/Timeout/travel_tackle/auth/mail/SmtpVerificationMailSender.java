package Timeout.travel_tackle.auth.mail;

import Timeout.travel_tackle.global.exception.CustomException;
import Timeout.travel_tackle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpVerificationMailSender implements VerificationMailSender {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from:no-reply@travel-tackle.local}")
    private String from;

    // 만료 시간을 설정값에서 읽어 본문에 넣는다 — 하드코딩 문구가 실제 만료와 어긋나는 것을 방지
    @Value("${app.auth.email-verification-expiration-minutes:10}")
    private long verificationExpirationMinutes;

    @Value("${app.auth.password-reset-expiration-minutes:10}")
    private long passwordResetExpirationMinutes;

    @Override
    public void sendVerificationCode(String email, String code) {
        send(email, "[트래블 참견] 이메일 인증번호",
                "안녕하세요, 트래블 참견입니다.\n\n"
                        + "인증번호는 " + code + "입니다. "
                        + verificationExpirationMinutes + "분 안에 입력해 주세요.",
                ErrorCode.EMAIL_DELIVERY_FAILED);
    }

    @Override
    public void sendPasswordResetCode(String email, String code) {
        send(email, "[트래블 참견] 비밀번호 재설정 인증번호",
                "안녕하세요, 트래블 참견입니다.\n\n"
                        + "비밀번호 재설정 인증번호는 " + code + "입니다. "
                        + passwordResetExpirationMinutes + "분 안에 입력해 주세요.\n\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시해 주세요.",
                ErrorCode.PASSWORD_RESET_DELIVERY_FAILED);
    }

    private void send(String to, String subject, String text, ErrorCode failureCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            log.error(
                    "SMTP email delivery failed: exceptionType={}, reason={}",
                    exception.getClass().getSimpleName(),
                    sanitize(exception.getMessage())
            );
            throw new CustomException(failureCode);
        }
    }

    private String sanitize(String message) {
        if (message == null) {
            return "unknown";
        }
        return message.replaceAll("(?i)(password|passwd|pwd)=[^,;\\s]+", "$1=***");
    }
}
