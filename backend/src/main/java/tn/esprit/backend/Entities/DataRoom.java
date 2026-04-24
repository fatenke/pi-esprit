package tn.esprit.backend.Entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "data_rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DataRoom {
    @Id
    private String id;
    private String startupId;
    private String investorId;
    private String dealId;
    private boolean ndaSigned;
    private String status; // ACTIVE / EXPIRED
    private LocalDateTime createdAt;
}
