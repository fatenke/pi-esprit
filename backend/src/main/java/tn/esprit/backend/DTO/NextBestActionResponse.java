package tn.esprit.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.backend.enums.ActionStatus;
import tn.esprit.backend.enums.ActorRole;
import tn.esprit.backend.enums.Priority;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextBestActionResponse {
    private String id;
    private String investmentRequestId;
    private ActorRole actorRole;
    private String title;
    private String description;
    private Priority priority;
    private String reason;
    private ActionStatus status;
    private Boolean aiGenerated;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
}
