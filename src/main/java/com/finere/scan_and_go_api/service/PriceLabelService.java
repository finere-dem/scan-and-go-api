package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.domain.entity.Order;
import com.finere.scan_and_go_api.domain.entity.OrderItem;
import com.finere.scan_and_go_api.domain.enums.OrgType;
import com.finere.scan_and_go_api.domain.enums.TargetOrgType;
import com.finere.scan_and_go_api.dto.qr.LabelItem;
import com.finere.scan_and_go_api.dto.qr.QrCodeResult;
import com.finere.scan_and_go_api.repository.LocalRetailPriceRepository;
import com.finere.scan_and_go_api.repository.OrderRepository;
import com.finere.scan_and_go_api.repository.PricingPolicyRepository;
import com.finere.scan_and_go_api.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generates the price labels a seller sticks onto each unit before delivery - the
 * one piece of the Ubipharm-style workflow this project was missing. The sale
 * price is already known at delivery time (the retailer fixed its consumer price,
 * or the wholesaler fixed its resale price to retailers, before ever placing the
 * order), so it can be printed straight onto the product rather than guessed at
 * the counter.
 */
@Service
@RequiredArgsConstructor
public class PriceLabelService {

    private final OrderRepository orderRepository;
    private final LocalRetailPriceRepository localRetailPriceRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final QrCodeGenerationService qrCodeGenerationService;
    private final PdfLabelSheetService pdfLabelSheetService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public byte[] generateForOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown order: " + orderId));
        requireBuyerOrSellerOrSuperAdmin(order);

        List<LabelItem> labelItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            String priceLabel = resolvePriceLabel(order, item);
            QrCodeResult qr = qrCodeGenerationService.generateForOrderFulfillment(item.getProduct(), item.getLot());

            for (int i = 0; i < item.getQuantity(); i++) {
                labelItems.add(new LabelItem(
                        item.getProduct().getName(),
                        item.getProduct().getSku(),
                        item.getLot() != null ? item.getLot().getLotNumber() : null,
                        qr.pngBytes(),
                        priceLabel));
            }
        }

        return pdfLabelSheetService.renderA4Sheet(labelItems);
    }

    /** The price to print is the one the buyer already committed to charging downstream:
     * a retailer's own consumer price if already set, or a wholesaler's own resale price
     * to retailers if already set. Neither existing is not an error - the label just omits
     * the price line rather than blocking delivery on a pricing decision the buyer hasn't made yet. */
    private String resolvePriceLabel(Order order, OrderItem item) {
        UUID buyerOrgId = order.getBuyerOrg().getId();
        UUID productId = item.getProduct().getId();
        OrgType buyerOrgType = order.getBuyerOrg().getOrgType();

        BigDecimal price = null;
        String currency = null;

        if (buyerOrgType == OrgType.RETAILER) {
            var retailPrice = localRetailPriceRepository.findByRetailerOrgIdAndProductId(buyerOrgId, productId);
            if (retailPrice.isPresent()) {
                price = retailPrice.get().getConsumerPrice();
                currency = retailPrice.get().getCurrency();
            }
        } else if (buyerOrgType == OrgType.WHOLESALER) {
            var resalePolicy = pricingPolicyRepository.findBySellerOrgIdAndProductIdAndTargetOrgType(
                    buyerOrgId, productId, TargetOrgType.RETAILER);
            if (resalePolicy.isPresent()) {
                price = resalePolicy.get().getUnitPrice();
                currency = resalePolicy.get().getCurrency();
            }
        }

        return price != null ? "Prix: " + price.toPlainString() + " " + currency : null;
    }

    private void requireBuyerOrSellerOrSuperAdmin(Order order) {
        if (currentUserService.isSuperAdmin()) {
            return;
        }
        UUID callerOrgId = currentUserService.requireOrgId();
        boolean isParty = callerOrgId.equals(order.getBuyerOrg().getId()) || callerOrgId.equals(order.getSellerOrg().getId());
        if (!isParty) {
            throw new AccessDeniedException("Organization " + callerOrgId + " is not a party to order " + order.getId());
        }
    }
}
