package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.InvestmentCriteria;

public interface InvestmentCriteriaRepo extends MongoRepository<InvestmentCriteria, String> {
}
