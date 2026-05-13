package tn.esprit.backend.Entities;

import lombok.Builder;
import lombok.Data;
import tn.esprit.backend.enums.DealStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusHistory {
    private DealStatus fromStatus;
    private DealStatus toStatus;
    private LocalDateTime changedAt;
    private String changedBy;
    private String reason;
}
