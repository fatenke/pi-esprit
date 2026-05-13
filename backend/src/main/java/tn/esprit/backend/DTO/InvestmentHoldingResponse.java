package tn.esprit.backend.DTO;

import lombok.Builder;
import lombok.Data;
import tn.esprit.backend.enums.InvestmentHoldingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvestmentHoldingResponse {
    private String id;
    private String investmentRequestId;
    private String investorId;
    private String startupId;
    private Long amountTnd;
    private BigDecimal amountEur;
    private String currencyDisplayed;
    private String stripeCurrency;
    private InvestmentHoldingStatus status;
    private String stripeCheckoutSessionId;
    private String stripePaymentIntentId;
    private LocalDateTime createdAt;
    private LocalDateTime fundedAt;
    private LocalDateTime releasedAt;
    private LocalDateTime cancelledAt;
    private List<InvestmentHoldingMilestoneResponse> milestones;
}
