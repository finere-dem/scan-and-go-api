package com.finere.scan_and_go_api.domain.enums;

public enum PaymentMode {
    CASH, ON_DELIVERY, CREDIT_30, CREDIT_60, CREDIT_90;

    public boolean isCredit() {
        return this == CREDIT_30 || this == CREDIT_60 || this == CREDIT_90;
    }

    public int termDays() {
        return switch (this) {
            case CREDIT_30 -> 30;
            case CREDIT_60 -> 60;
            case CREDIT_90 -> 90;
            default -> 0;
        };
    }
}
