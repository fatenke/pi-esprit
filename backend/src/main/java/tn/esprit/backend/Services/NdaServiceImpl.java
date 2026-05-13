package tn.esprit.backend.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.backend.DTO.NdaAgreementResponse;
import tn.esprit.backend.DTO.NdaSignResponse;
import tn.esprit.backend.DTO.NdaStatusResponse;
import tn.esprit.backend.DTO.SignNdaRequest;
import tn.esprit.backend.Entities.DataRoom;
import tn.esprit.backend.Entities.NdaAgreement;
import tn.esprit.backend.Entities.NdaSignature;
import tn.esprit.backend.enums.NdaStatus;
import tn.esprit.backend.enums.SignatureType;
import tn.esprit.backend.Repositories.DataRoomRepo;
import tn.esprit.backend.Repositories.NdaAgreementRepository;
import tn.esprit.backend.Repositories.NdaSignatureRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NdaServiceImpl implements NdaService {
    private static final String CERTIFICATE_ENDPOINT = "/api/dataroom/%s/nda/certificate";
    private static final String SIGNATURE_IMAGE_ENDPOINT = "/api/dataroom/%s/nda/signatures/%s/image";

    private final DataRoomRepo dataRoomRepo;
    private final NdaAgreementRepository ndaAgreementRepository;
    private final NdaSignatureRepository ndaSignatureRepository;

    @Override
    public NdaAgreement createOrGetNda(String dataRoomId) {
        return ndaAgreementRepository.findByDataRoomId(dataRoomId)
                .orElseGet(() -> createAgreement(loadRoom(dataRoomId)));
    }

    @Override
    public NdaAgreementResponse getNda(String dataRoomId, RequestActor actor) {
        NdaAgreement agreement = createOrGetNda(dataRoomId);
        validateRoomMembership(loadRoom(dataRoomId), actor, true);
        return toAgreementResponse(agreement);
    }

    @Override
    public NdaSignResponse signNda(String dataRoomId, SignNdaRequest request, RequestActor actor, String ipAddress, String userAgent) {
        DataRoom room = loadRoom(dataRoomId);
        NdaAgreement agreement = createOrGetNda(dataRoomId);

        if (actor.getRole() != UserRole.INVESTOR) {
            throw new SecurityException("Only the linked investor can sign this NDA.");
        }
        if (!Objects.equals(room.getInvestorId(), actor.getUserId())) {
            throw new SecurityException("Only the investor linked to this data room can sign the NDA.");
        }
        if (agreement.getStatus() == NdaStatus.SIGNED) {
            throw new IllegalStateException("This NDA has already been signed.");
        }
        if (!request.isAcceptedTerms()) {
            throw new IllegalArgumentException("You must accept the NDA terms before signing.");
        }

        String signerFullName = safeTrim(request.getSignerFullName());
        if (signerFullName.isBlank()) {
            throw new IllegalArgumentException("The signer full name is required.");
        }

        SignatureType signatureType = request.getSignatureType();
        if (signatureType == null) {
            throw new IllegalArgumentException("The signature type is required.");
        }
        if (signatureType != SignatureType.DRAWN) {
            throw new IllegalArgumentException("Only drawn signatures are allowed for this NDA.");
        }

        String signatureImageUrl = null;
        String typedSignature = null;
        if (safeTrim(request.getSignatureImageBase64()).isBlank()) {
            throw new IllegalArgumentException("A drawn signature image is required.");
        }

        LocalDateTime signedAt = LocalDateTime.now();
        String signatureHash = generateSignatureHash(
                agreement.getNdaHash(),
                actor.getUserId(),
                signedAt.toString(),
                signerFullName
        );

        NdaSignature signature = NdaSignature.builder()
                .ndaAgreementId(agreement.getId())
                .signerId(actor.getUserId())
                .signerFullName(signerFullName)
                .signatureType(signatureType)
                .typedSignature(typedSignature)
                .ipAddress(blankToNull(ipAddress))
                .userAgent(blankToNull(userAgent))
                .signedAt(signedAt)
                .signatureHash(signatureHash)
                .certificateUrl(String.format(CERTIFICATE_ENDPOINT, dataRoomId))
                .build();
        signature = ndaSignatureRepository.save(signature);

        signatureImageUrl = saveSignatureImage(dataRoomId, signature.getId(), request.getSignatureImageBase64());
        signature.setSignatureImageUrl(signatureImageUrl);
        signature = ndaSignatureRepository.save(signature);

        agreement.setStatus(NdaStatus.SIGNED);
        agreement.setSignedAt(signedAt);
        ndaAgreementRepository.save(agreement);

        room.setNdaSigned(true);
        dataRoomRepo.save(room);

        return NdaSignResponse.builder()
                .ndaId(agreement.getId())
                .status(agreement.getStatus())
                .signedAt(signedAt)
                .signatureHash(signatureHash)
                .certificateUrl(signature.getCertificateUrl())
                .message("NDA signed successfully")
                .build();
    }

    @Override
    public String generateNdaHash(String ndaContent) {
        return sha256Hex(ndaContent.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateSignatureHash(String ndaHash, String signerId, String signedAt, String signerFullName) {
        String payload = safeTrim(ndaHash) + safeTrim(signerId) + safeTrim(signedAt) + safeTrim(signerFullName);
        return sha256Hex(payload.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean canAccessDataRoom(String dataRoomId, RequestActor actor) {
        DataRoom room = loadRoom(dataRoomId);
        validateRoomMembership(room, actor, false);

        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.STARTUP) {
            return true;
        }

        NdaAgreement agreement = createOrGetNda(dataRoomId);
        return agreement.getStatus() == NdaStatus.SIGNED && Objects.equals(room.getInvestorId(), actor.getUserId());
    }

    @Override
    public NdaStatusResponse getNdaStatus(String dataRoomId, RequestActor actor) {
        NdaAgreement agreement = createOrGetNda(dataRoomId);
        validateRoomMembership(loadRoom(dataRoomId), actor, true);
        return NdaStatusResponse.builder()
                .dataRoomId(dataRoomId)
                .status(agreement.getStatus())
                .canAccessDataRoom(canAccessDataRoom(dataRoomId, actor))
                .signedAt(agreement.getSignedAt())
                .build();
    }

    @Override
    public NdaSignature getSignatureForCertificate(String dataRoomId, RequestActor actor) {
        NdaAgreement agreement = createOrGetNda(dataRoomId);
        validateRoomMembership(loadRoom(dataRoomId), actor, true);
        if (agreement.getStatus() != NdaStatus.SIGNED) {
            throw new IllegalStateException("The NDA is not signed yet.");
        }
        return ndaSignatureRepository.findTopByNdaAgreementIdOrderBySignedAtDesc(agreement.getId())
                .orElseThrow(() -> new IllegalStateException("The NDA signature proof was not found."));
    }

    private NdaAgreement createAgreement(DataRoom room) {
        String ndaContent = standardContent(room);
        NdaAgreement agreement = NdaAgreement.builder()
                .dataRoomId(room.getId())
                .investorId(room.getInvestorId())
                .startupId(room.getStartupId())
                .ndaContent(ndaContent)
                .ndaHash(generateNdaHash(ndaContent))
                .status(NdaStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .signedAt(null)
                .build();
        return ndaAgreementRepository.save(agreement);
    }

    private String standardContent(DataRoom room) {
        return """
                ACCORD DE CONFIDENTIALITE (NDA)

                Cet espace de data room contient des documents confidentiels communiques dans le cadre d'une evaluation d'investissement academique.

                La startup %s autorise l'investisseur %s a consulter ces documents uniquement pour analyser le deal associe.

                Le signataire s'engage a :
                1. conserver strictement confidentielles les informations partagees ;
                2. ne pas copier, diffuser ou transmettre les documents sans autorisation prealable ;
                3. utiliser les informations uniquement dans le cadre de l'evaluation de l'investissement ;
                4. respecter les restrictions de la plateforme et les journaux d'acces.

                Ce mecanisme constitue une signature virtuelle simulee pour un projet academique et ne remplace pas une signature legale certifiee.
                """.formatted(room.getStartupId(), room.getInvestorId()).trim();
    }

    private String saveSignatureImage(String dataRoomId, String signatureId, String dataUrl) {
        byte[] bytes = decodeDataUrl(dataUrl);
        try {
            Path directory = Path.of("storage", "nda-signatures");
            Files.createDirectories(directory);
            Path file = directory.resolve("%s-%s.png".formatted(dataRoomId, signatureId));
            Files.write(file, bytes);
            return String.format(SIGNATURE_IMAGE_ENDPOINT, dataRoomId, signatureId);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store the signature image.", e);
        }
    }

    private byte[] decodeDataUrl(String dataUrl) {
        String value = safeTrim(dataUrl);
        int commaIndex = value.indexOf(',');
        String base64 = commaIndex >= 0 ? value.substring(commaIndex + 1) : value;
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("The drawn signature image is invalid.");
        }
    }

    private String sha256Hex(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }

    private NdaAgreementResponse toAgreementResponse(NdaAgreement agreement) {
        return NdaAgreementResponse.builder()
                .id(agreement.getId())
                .dataRoomId(agreement.getDataRoomId())
                .status(agreement.getStatus())
                .ndaContent(agreement.getNdaContent())
                .ndaHash(agreement.getNdaHash())
                .signedAt(agreement.getSignedAt())
                .build();
    }

    private DataRoom loadRoom(String dataRoomId) {
        return dataRoomRepo.findById(dataRoomId)
                .orElseThrow(() -> new IllegalArgumentException("The requested data room was not found."));
    }

    private void validateRoomMembership(DataRoom room, RequestActor actor, boolean allowPendingInvestor) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.STARTUP && Objects.equals(room.getStartupId(), actor.getUserId())) {
            return;
        }
        if (actor.getRole() == UserRole.INVESTOR && Objects.equals(room.getInvestorId(), actor.getUserId())) {
            if (allowPendingInvestor || canAccessInvestorPending(room.getId())) {
                return;
            }
        }
        throw new SecurityException("You are not allowed to access this data room.");
    }

    private boolean canAccessInvestorPending(String dataRoomId) {
        NdaAgreement agreement = createOrGetNda(dataRoomId);
        return agreement.getStatus() == NdaStatus.SIGNED;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        String trimmed = safeTrim(value);
        return trimmed.isBlank() ? null : trimmed;
    }
}
