package tn.esprit.backend.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.backend.enums.NdaStatus;

import java.time.LocalDateTime;

@Document(collection = "nda_agreements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NdaAgreement {
    @Id
    private String id;
    private String dataRoomId;
    private String investorId;
    private String startupId;
    private String ndaContent;
    private String ndaHash;
    private NdaStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime signedAt;
}
