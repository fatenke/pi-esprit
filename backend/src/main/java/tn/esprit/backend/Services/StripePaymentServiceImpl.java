package tn.esprit.backend.Services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.backend.config.StripeProperties;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripePaymentServiceImpl implements StripePaymentService {
    private final StripeProperties stripeProperties;

    @Override
    public StripePaymentIntentData createPaymentIntent(Long amount, String currency, String requestId, String investorId, String startupId) {
        try {
            Stripe.apiKey = stripeProperties.getSecretKey();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putMetadata("investmentRequestId", requestId)
                    .putMetadata("investorId", investorId)
                    .putMetadata("startupId", startupId)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return new StripePaymentIntentData(paymentIntent.getId(), paymentIntent.getClientSecret());
        } catch (StripeException e) {
            throw new IllegalStateException("Unable to create Stripe test payment intent: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPaymentIntentStatus(String paymentIntentId) {
        try {
            Stripe.apiKey = stripeProperties.getSecretKey();
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return paymentIntent.getStatus();
        } catch (StripeException e) {
            throw new IllegalStateException("Unable to verify Stripe payment intent: " + e.getMessage(), e);
        }
    }
}
