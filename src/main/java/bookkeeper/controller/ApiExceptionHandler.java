package bookkeeper.controller;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final MeterRegistry meterRegistry;

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Map<String, String>> conflict(ConcurrencyFailureException e) {
        meterRegistry.counter("ledger.transfer.failures",
                "type", e.getClass().getSimpleName()).increment();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "concurrent update, retry"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> unprocessable(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}