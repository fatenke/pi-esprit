package tn.esprit.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextBestActionAiResult {
    private String actorRole;
    private String title;
    private String description;
    private String priority;
    private String reason;
    private Integer dueInDays;
}
