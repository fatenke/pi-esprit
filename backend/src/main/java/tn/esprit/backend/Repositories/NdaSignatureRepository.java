package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.NdaSignature;

import java.util.Optional;

public interface NdaSignatureRepository extends MongoRepository<NdaSignature, String> {
    Optional<NdaSignature> findTopByNdaAgreementIdOrderBySignedAtDesc(String ndaAgreementId);
    Optional<NdaSignature> findByNdaAgreementId(String ndaAgreementId);
}
