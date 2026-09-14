package Timeout.travel_tackle.global;

import Timeout.travel_tackle.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GlobalExceptionHandlerTests {

    @Test
    void clientDisconnectDuringSseIsSwallowedWithoutWritingAResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications/stream");

        assertDoesNotThrow(() -> handler.handleClientDisconnected(
                new AsyncRequestNotUsableException("Servlet container error notification for disconnected client",
                        new IOException("Broken pipe")), request));
    }
}
