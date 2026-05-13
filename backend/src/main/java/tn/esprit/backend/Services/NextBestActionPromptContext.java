package tn.esprit.backend.Services;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextBestActionPromptContext {
    private String investmentRequestId;
    private String investorId;
    private String startupId;
    private String investmentStatus;
    private Long ticketProposed;
    private String introMessage;
    private String dealStatus;
    private long daysInStatus;
    private LocalDateTime statusChangedAt;
    private List<String> missingDataRoomFolders;
    private int availableDataRoomDocuments;
    private boolean ndaSigned;
    private String holdingStatus;
    private Long holdingAmountTnd;
    private BigDecimal holdingAmountEur;
    private LocalDateTime holdingFundedAt;
}
