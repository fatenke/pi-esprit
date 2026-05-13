package tn.esprit.backend.Services;

import tn.esprit.backend.DTO.NdaAgreementResponse;
import tn.esprit.backend.DTO.NdaSignResponse;
import tn.esprit.backend.DTO.NdaStatusResponse;
import tn.esprit.backend.DTO.SignNdaRequest;
import tn.esprit.backend.Entities.NdaAgreement;
import tn.esprit.backend.Entities.NdaSignature;

public interface NdaService {
    NdaAgreement createOrGetNda(String dataRoomId);
    NdaAgreementResponse getNda(String dataRoomId, RequestActor actor);
    NdaSignResponse signNda(String dataRoomId, SignNdaRequest request, RequestActor actor, String ipAddress, String userAgent);
    String generateNdaHash(String ndaContent);
    String generateSignatureHash(String ndaHash, String signerId, String signedAt, String signerFullName);
    boolean canAccessDataRoom(String dataRoomId, RequestActor actor);
    NdaStatusResponse getNdaStatus(String dataRoomId, RequestActor actor);
    NdaSignature getSignatureForCertificate(String dataRoomId, RequestActor actor);
}
