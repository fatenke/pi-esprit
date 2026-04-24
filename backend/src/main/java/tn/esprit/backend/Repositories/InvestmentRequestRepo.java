package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.InvestmentRequest;

import java.util.List;
import java.util.Optional;

public interface InvestmentRequestRepo extends MongoRepository<InvestmentRequest, String> {
    List<InvestmentRequest> findByInvestorId(String investorId);
    List<InvestmentRequest> findByStartupId(String startupId);
}
