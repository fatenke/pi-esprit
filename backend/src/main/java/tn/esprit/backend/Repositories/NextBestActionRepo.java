package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.NextBestAction;

import java.util.List;

public interface NextBestActionRepo extends MongoRepository<NextBestAction, String> {
    List<NextBestAction> findByInvestmentRequestIdOrderByCreatedAtDesc(String investmentRequestId);
}
