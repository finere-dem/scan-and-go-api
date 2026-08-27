package com.finere.scan_and_go_api.dto.qr;

/** One printable label: product identity plus the QR PNG already rendered for it. */
public record LabelItem(
        String productName,
        String sku,
        String lotNumber,
        byte[] qrPng
) {
}
