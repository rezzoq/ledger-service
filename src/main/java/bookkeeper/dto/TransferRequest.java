package bookkeeper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull Long fromAccId,
        @NotNull Long toAccId,
        @NotNull @Positive
        BigDecimal amount,
        @NotBlank
        @Size(max = 64) String idempotencyKey)
{ }
