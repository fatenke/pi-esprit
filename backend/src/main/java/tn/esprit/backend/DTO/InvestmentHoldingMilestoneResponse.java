package tn.esprit.backend.DTO;

import lombok.Builder;
import lombok.Data;
import tn.esprit.backend.enums.InvestmentMilestoneStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class InvestmentHoldingMilestoneResponse {
    private String id;
    private String holdingId;
    private String title;
    private Long amount;
    private LocalDateTime dueDate;
    private InvestmentMilestoneStatus status;
    private boolean validatedByStartup;
    private boolean validatedByInvestor;
    private LocalDateTime releasedAt;
}
