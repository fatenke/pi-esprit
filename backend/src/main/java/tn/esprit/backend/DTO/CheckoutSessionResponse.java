package tn.esprit.backend.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CheckoutSessionResponse {
    private String holdingId;
    private Long amountTnd;
    private BigDecimal amountEur;
    private String checkoutUrl;
}
