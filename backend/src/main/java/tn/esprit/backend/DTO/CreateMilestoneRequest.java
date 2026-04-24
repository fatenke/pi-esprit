package tn.esprit.backend.DTO;

import lombok.Data;

@Data
public class CreateMilestoneRequest {
    private String title;
    private Long amount;
    private String dueDate;
}
