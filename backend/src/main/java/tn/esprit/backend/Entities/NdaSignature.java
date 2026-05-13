package tn.esprit.backend.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.backend.enums.SignatureType;

import java.time.LocalDateTime;

@Document(collection = "nda_signatures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NdaSignature {
    @Id
    private String id;
    private String ndaAgreementId;
    private String signerId;
    private String signerFullName;
    private SignatureType signatureType;
    private String signatureImageUrl;
    private String typedSignature;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime signedAt;
    private String signatureHash;
    private String certificateUrl;
}
