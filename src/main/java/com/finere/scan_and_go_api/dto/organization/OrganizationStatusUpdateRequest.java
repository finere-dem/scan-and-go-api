package com.finere.scan_and_go_api.dto.organization;

import com.finere.scan_and_go_api.domain.enums.OrgStatus;
import jakarta.validation.constraints.NotNull;

public record OrganizationStatusUpdateRequest(
        @NotNull(message = "Status is required")
        OrgStatus status
) {
}
