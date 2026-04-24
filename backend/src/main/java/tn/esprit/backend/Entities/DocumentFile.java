package tn.esprit.backend.Entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DocumentFile {
    @Id
    private String id;
    private String roomId;
    private String fileName;
    private String folder; // FINANCIAL, LEGAL...
    private String storageId;
    private String fileUrl;
    private String contentType;
    private String type;
    private LocalDateTime createdAt;
}
