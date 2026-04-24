package tn.esprit.backend.Entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "investment_criteria")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InvestmentCriteria {
    @Id
    private String id;

    //@NotNull(message = "Investor ID is required")
    private String investorId;
    private List<String> sectors;
    private List<String> stage;

    //@Min(value = 0, message = "Minimum budget must be positive")
    private double minBudget;

    //@Min(value = 0, message = "Maximum budget must be positive")
    private double maxBudget;

    private String location;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
