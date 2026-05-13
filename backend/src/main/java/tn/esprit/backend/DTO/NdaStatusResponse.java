package tn.esprit.backend.DTO;

import lombok.Builder;
import lombok.Data;
import tn.esprit.backend.enums.NdaStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class NdaStatusResponse {
    private String dataRoomId;
    private NdaStatus status;
    private boolean canAccessDataRoom;
    private LocalDateTime signedAt;
}
