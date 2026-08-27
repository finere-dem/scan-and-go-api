package com.finere.scan_and_go_api.dto.order;

import java.util.UUID;

/** One slice of a FEFO allocation: this many units taken from this specific lot. */
public record LotAllocation(UUID lotId, int quantity) {
}
