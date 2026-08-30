package com.finere.scan_and_go_api.dto.product;

import com.finere.scan_and_go_api.domain.enums.LotStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductLotResponse(
        UUID id,
        UUID productId,
        UUID warehouseId,
        UUID boutiqueId,
        String lotNumber,
        LocalDate mfgDate,
        LocalDate expDate,
        int initialQuantity,
        int currentQuantity,
        BigDecimal unitCost,
        LotStatus status
) {
}
