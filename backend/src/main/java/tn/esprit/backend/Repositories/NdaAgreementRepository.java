package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.NdaAgreement;

import java.util.Optional;

public interface NdaAgreementRepository extends MongoRepository<NdaAgreement, String> {
    Optional<NdaAgreement> findByDataRoomId(String dataRoomId);
}
