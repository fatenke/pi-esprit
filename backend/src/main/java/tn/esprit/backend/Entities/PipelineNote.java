package tn.esprit.backend.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineNote {

    private String id;          // UUID genere manuellement
    private String content;     // texte de la note, max 500 chars
    private LocalDateTime createdAt;
}
