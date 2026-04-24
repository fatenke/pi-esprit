package tn.esprit.backend.DTO;

import lombok.Data;

@Data
public class CreateInvestmentHoldingRequest {
    private Long amount;
    private String currency;
}
