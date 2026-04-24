package tn.esprit.backend.Services;

public interface StripePaymentService {
    StripePaymentIntentData createPaymentIntent(Long amount, String currency, String requestId, String investorId, String startupId);
    String getPaymentIntentStatus(String paymentIntentId);
}
