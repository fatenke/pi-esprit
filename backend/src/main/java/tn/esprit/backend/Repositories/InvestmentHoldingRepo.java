package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.InvestmentHolding;

import java.util.List;
import java.util.Optional;

public interface InvestmentHoldingRepo extends MongoRepository<InvestmentHolding, String> {
    Optional<InvestmentHolding> findByInvestmentRequestId(String investmentRequestId);
    Optional<InvestmentHolding> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);
    List<InvestmentHolding> findByInvestorId(String investorId);
    List<InvestmentHolding> findByStartupId(String startupId);
}
