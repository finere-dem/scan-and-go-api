package com.finere.scan_and_go_api.dto.qr;

/** One printable label: product identity plus the QR PNG already rendered for it.
 * {@code priceLabel} is set only for order-delivery labels (e.g. "Prix: 550 XOF") -
 * the sale price is already known at delivery time since the buyer fixed it (or the
 * seller quoted it) before ordering, so it can be printed straight onto the product. */
public record LabelItem(
        String productName,
        String sku,
        String lotNumber,
        byte[] qrPng,
        String priceLabel
) {
    public LabelItem(String productName, String sku, String lotNumber, byte[] qrPng) {
        this(productName, sku, lotNumber, qrPng, null);
    }
}
