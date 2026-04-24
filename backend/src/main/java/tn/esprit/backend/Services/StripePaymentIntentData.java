package tn.esprit.backend.Services;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StripePaymentIntentData {
    private String paymentIntentId;
    private String clientSecret;
}
