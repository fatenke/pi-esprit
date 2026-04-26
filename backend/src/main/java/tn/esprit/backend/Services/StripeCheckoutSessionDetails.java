package tn.esprit.backend.Services;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StripeCheckoutSessionDetails {
    private final String sessionId;
    private final String paymentStatus;
    private final String paymentIntentId;
}
