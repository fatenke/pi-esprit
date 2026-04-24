package tn.esprit.backend.DTO;

import lombok.Builder;
import lombok.Data;
import tn.esprit.backend.Entities.DocumentFile;

import java.util.List;

@Builder
@Data
public class DataRoomResponse {
    private String roomId;
    private boolean ndaSigned;
    private List<DocumentFile> documents;
}
