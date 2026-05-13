package tn.esprit.backend.DTO;

import lombok.Builder;
import lombok.Data;
import tn.esprit.backend.enums.NdaStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class NdaAgreementResponse {
    private String id;
    private String dataRoomId;
    private NdaStatus status;
    private String ndaContent;
    private String ndaHash;
    private LocalDateTime signedAt;
}
