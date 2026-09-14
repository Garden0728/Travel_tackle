package Timeout.travel_tackle.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInput(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        log.warn("Invalid request: path={}, type={}", request.getRequestURI(), exception.getClass().getSimpleName());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.IMAGE_TOO_LARGE;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    // SSE 등 비동기 응답 중 클라이언트가 끊기면 컨테이너가 이 예외를 통보한다. 응답을 쓸 수 없는 상태이므로
    // 포괄 핸들러가 ErrorResponse 를 쓰려다 두 번째 예외를 내지 않도록 여기서 조용히 끝낸다.
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientDisconnected(AsyncRequestNotUsableException exception, HttpServletRequest request) {
        log.debug("Client disconnected during async response: path={}", request.getRequestURI());
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(
            CustomException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("Handled custom exception: code={}, path={}", errorCode.getCode(), request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("Unhandled exception: path={}", request.getRequestURI(), exception);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }
}
