package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Product;
import com.finere.scan_and_go_api.domain.entity.ProductLot;
import com.finere.scan_and_go_api.domain.entity.QrMatrixToken;
import com.finere.scan_and_go_api.domain.enums.LotStatus;
import com.finere.scan_and_go_api.domain.enums.QrMatrixType;
import com.finere.scan_and_go_api.dto.qr.LabelItem;
import com.finere.scan_and_go_api.dto.qr.QrCodeResult;
import com.finere.scan_and_go_api.repository.ProductLotRepository;
import com.finere.scan_and_go_api.repository.ProductRepository;
import com.finere.scan_and_go_api.repository.QrMatrixTokenRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates the vectorial QR payload used across product/lot labels and shelf posters:
 * {@code SCAN_GO://P/{public_token}?sig={hmac_sha256}}. The signature lets a scanning device
 * (mobile or in-store terminal) verify the token wasn't tampered with before hitting the API.
 */
@Service
@RequiredArgsConstructor
public class QrCodeGenerationService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int PNG_SIZE_PX = 400;

    private final QrMatrixTokenRepository qrMatrixTokenRepository;
    private final ProductRepository productRepository;
    private final ProductLotRepository productLotRepository;
    private final QrProperties qrProperties;
    private final PdfLabelSheetService pdfLabelSheetService;
    private final CurrentUserService currentUserService;

    @Transactional
    public QrCodeResult generateForProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + productId));
        currentUserService.requireSameOrgOrSuperAdmin(product.getImporter().getId());
        return generate(product, null, QrMatrixType.PRODUCT_GLOBAL);
    }

    @Transactional
    public QrCodeResult generateForLot(UUID lotId) {
        ProductLot lot = productLotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown lot: " + lotId));
        currentUserService.requireSameOrgOrSuperAdmin(lot.getProduct().getImporter().getId());
        return generate(lot.getProduct(), lot, QrMatrixType.LOT_SPECIFIC);
    }

    @Transactional
    public QrCodeResult generateShelfPoster(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + productId));
        currentUserService.requireSameOrgOrSuperAdmin(product.getImporter().getId());
        return generate(product, null, QrMatrixType.SHELF_POSTER);
    }

    /** Builds a printable A4 label sheet with one lot-specific QR label per active lot of the product. */
    @Transactional
    public byte[] buildLabelSheetForProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + productId));
        currentUserService.requireSameOrgOrSuperAdmin(product.getImporter().getId());

        List<ProductLot> activeLots = productLotRepository.findByProductId(productId).stream()
                .filter(lot -> lot.getStatus() == LotStatus.ACTIVE)
                .toList();

        if (activeLots.isEmpty()) {
            throw new IllegalArgumentException("No active lots found for product " + productId);
        }

        List<LabelItem> labelItems = activeLots.stream()
                .map(lot -> {
                    QrCodeResult qr = generate(lot.getProduct(), lot, QrMatrixType.LOT_SPECIFIC);
                    return new LabelItem(lot.getProduct().getName(), lot.getProduct().getSku(), lot.getLotNumber(), qr.pngBytes());
                })
                .toList();

        return pdfLabelSheetService.renderA4Sheet(labelItems);
    }

    /** Used when a QR is generated as part of order fulfillment rather than catalog management -
     * authorization there is based on the order's buyer/seller org, not the product's importer,
     * so it's the caller's job to authorize before calling this. */
    @Transactional
    public QrCodeResult generateForOrderFulfillment(Product product, ProductLot lot) {
        return generate(product, lot, QrMatrixType.LOT_SPECIFIC);
    }

    private QrCodeResult generate(Product product, ProductLot lot, QrMatrixType matrixType) {
        String publicToken = UUID.randomUUID().toString().replace("-", "");
        String signature = sign(publicToken);

        QrMatrixToken token = new QrMatrixToken();
        token.setProduct(product);
        token.setLot(lot);
        token.setPublicToken(publicToken);
        token.setSignatureHash(signature);
        token.setMatrixType(matrixType);
        qrMatrixTokenRepository.save(token);

        String payload = qrProperties.baseUri() + publicToken + "?sig=" + signature;
        byte[] png = renderPng(payload);

        return new QrCodeResult(token.getId(), publicToken, payload, png);
    }

    private String sign(String publicToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(qrProperties.hmacSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] rawHmac = mac.doFinal(publicToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to sign QR token", e);
        }
    }

    private byte[] renderPng(String payload) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN, 1);
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, PNG_SIZE_PX, PNG_SIZE_PX, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new UncheckedIOException("Unable to render QR code PNG", new IOException(e));
        }
    }
}
