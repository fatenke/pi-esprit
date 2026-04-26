package tn.esprit.backend.Services;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StripeCheckoutSessionData {
    private final String sessionId;
    private final String checkoutUrl;
}
