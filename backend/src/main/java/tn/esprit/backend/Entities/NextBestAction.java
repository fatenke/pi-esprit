package tn.esprit.backend.Entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.backend.enums.ActionStatus;
import tn.esprit.backend.enums.ActorRole;
import tn.esprit.backend.enums.Priority;

import java.time.LocalDateTime;

@Document(collection = "NextBestAction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class NextBestAction {
    @Id
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

