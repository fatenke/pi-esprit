package tn.esprit.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class StripeProperties {
    @Value("${stripe.secret.key:${STRIPE_SECRET_KEY:}}")
    private String secretKey;

    @Value("${stripe.public.key:${STRIPE_PUBLIC_KEY:}}")
    private String publicKey;

    @PostConstruct
    public void validate() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key is required.");
        }
        if (secretKey.startsWith("sk_live_")) {
            throw new IllegalStateException("Live Stripe secret keys are forbidden. Use sk_test_ only.");
        }
        if (!secretKey.startsWith("sk_test_")) {
            throw new IllegalStateException("Stripe secret key must start with sk_test_.");
        }
        if (publicKey != null && !publicKey.isBlank() && !publicKey.startsWith("pk_test_")) {
            throw new IllegalStateException("Stripe public key must start with pk_test_.");
        }
    }
}
