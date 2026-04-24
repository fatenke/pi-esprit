package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.InvestmentHoldingMilestone;

import java.util.List;

public interface InvestmentHoldingMilestoneRepo extends MongoRepository<InvestmentHoldingMilestone, String> {
    List<InvestmentHoldingMilestone> findByHoldingIdOrderByDueDateAsc(String holdingId);
}
