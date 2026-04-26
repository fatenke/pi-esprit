package tn.esprit.backend.Services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.backend.config.StripeProperties;

@Service
@RequiredArgsConstructor
public class StripePaymentServiceImpl implements StripePaymentService {
    private final StripeProperties stripeProperties;

    @Override
    public StripeCheckoutSessionData createCheckoutSession(long amountInCentsEur, String requestId, String investorId, String startupId) {
        try {
            Stripe.apiKey = stripeProperties.getSecretKey();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(stripeProperties.getSuccessUrl())
                    .setCancelUrl(stripeProperties.getCancelUrl())
                    .putMetadata("investmentRequestId", requestId)
                    .putMetadata("investorId", investorId)
                    .putMetadata("startupId", startupId)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(amountInCentsEur)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Investment Holding")
                                                                    .setDescription("Investment request " + requestId)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            return new StripeCheckoutSessionData(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new IllegalStateException("Unable to create Stripe Checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    public StripeCheckoutSessionDetails getCheckoutSessionDetails(String sessionId) {
        try {
            Stripe.apiKey = stripeProperties.getSecretKey();
            Session session = Session.retrieve(sessionId);
            return new StripeCheckoutSessionDetails(
                    session.getId(),
                    session.getPaymentStatus(),
                    session.getPaymentIntent()
            );
        } catch (StripeException e) {
            throw new IllegalStateException("Unable to verify Stripe Checkout session: " + e.getMessage(), e);
        }
    }
}
