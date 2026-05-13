package tn.esprit.backend.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.backend.enums.InvestmentHoldingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "investment_holding")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentHolding {
    @Id
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
}
