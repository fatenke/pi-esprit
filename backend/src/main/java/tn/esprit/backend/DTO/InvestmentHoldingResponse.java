package tn.esprit.backend.DTO;

import lombok.Builder;
import lombok.Data;
import tn.esprit.backend.Entities.InvestmentHoldingStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvestmentHoldingResponse {
    private String id;
    private String investmentRequestId;
    private String investorId;
    private String startupId;
    private Long amount;
    private String currency;
    private InvestmentHoldingStatus status;
    private String stripePaymentIntentId;
    private String stripeClientSecret;
    private LocalDateTime createdAt;
    private LocalDateTime fundedAt;
    private LocalDateTime releasedAt;
    private LocalDateTime cancelledAt;
    private List<InvestmentHoldingMilestoneResponse> milestones;
}
