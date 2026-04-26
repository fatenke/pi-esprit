package tn.esprit.backend.Services;

public interface StripePaymentService {
    StripeCheckoutSessionData createCheckoutSession(long amountInCentsEur, String requestId, String investorId, String startupId);
    StripeCheckoutSessionDetails getCheckoutSessionDetails(String sessionId);
}
