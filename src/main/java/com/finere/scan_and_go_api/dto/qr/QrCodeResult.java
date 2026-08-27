package com.finere.scan_and_go_api.dto.qr;

import java.util.UUID;

public record QrCodeResult(
        UUID tokenId,
        String publicToken,
        String payload,
        byte[] pngBytes
) {
}
