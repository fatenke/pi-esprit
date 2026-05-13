package tn.esprit.backend.Entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.backend.enums.DealStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Document(collection = "deal_pipeline")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DealPipeline {
    @Id
    private String id;
    private String investorId;
    private String requestId;
    private String startupId;
    private DealStatus status;
    private int columnOrder;
    private List<PipelineNote> privateNotes;
    private Boolean archivedByInvestor = false;
    private Boolean archivedByStartup = false;
    private LocalDateTime closedAt;
    private String nextAction;
    private LocalDateTime nextActionDate;
    private LocalDateTime statusChangedAt;
    private List<StatusHistory> history;
    public long getDaysInCurrentStatus() {
        return ChronoUnit.DAYS.between(statusChangedAt, LocalDateTime.now());
    }
}
