package tn.esprit.backend.DTO;

import lombok.Builder;
import lombok.Data;
import tn.esprit.backend.enums.NdaStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class NdaSignResponse {
    private String ndaId;
    private NdaStatus status;
    private LocalDateTime signedAt;
    private String signatureHash;
    private String certificateUrl;
    private String message;
}
