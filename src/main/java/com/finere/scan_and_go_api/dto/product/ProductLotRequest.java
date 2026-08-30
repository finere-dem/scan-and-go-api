package com.finere.scan_and_go_api.dto.product;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Exactly one of warehouseId/boutiqueId must be provided - a lot lives in one
 * or the other, never both, never neither. */
public record ProductLotRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        UUID warehouseId,

        UUID boutiqueId,

        @NotBlank(message = "Lot number is required")
        String lotNumber,

        @NotNull(message = "Manufacturing date is required")
        @PastOrPresent(message = "Manufacturing date cannot be in the future")
        LocalDate mfgDate,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        LocalDate expDate,

        @Min(value = 1, message = "Initial quantity must be at least 1")
        int initialQuantity,

        BigDecimal unitCost
) {
}
