package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.DealPipeline;
import tn.esprit.backend.enums.DealStatus;

import java.util.List;

public interface DealPipelineRepo extends MongoRepository<DealPipeline, String> {

    // Kanban par investisseur
    List<DealPipeline> findByInvestorIdOrderByColumnOrderAsc(String investorId);

    // Deals actifs
    List<DealPipeline> findByStatusIn(List<DealStatus> statuses);
    List<DealPipeline> findAllByRequestId(String requestId);
}
