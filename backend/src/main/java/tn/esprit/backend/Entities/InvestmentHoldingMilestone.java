package tn.esprit.backend.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.backend.enums.InvestmentMilestoneStatus;

import java.time.LocalDateTime;

@Document(collection = "investment_holding_milestone")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentHoldingMilestone {
    @Id
    private String id;
    private String holdingId;
    private String title;
    private Long amount;
    private LocalDateTime dueDate;
    private InvestmentMilestoneStatus status;
    private boolean validatedByStartup;
    private boolean validatedByInvestor;
    private LocalDateTime releasedAt;
}
