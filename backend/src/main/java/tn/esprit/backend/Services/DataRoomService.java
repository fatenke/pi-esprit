package tn.esprit.backend.Services;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend.DTO.DataRoomResponse;
import tn.esprit.backend.Entities.DataRoom;
import tn.esprit.backend.Entities.DocumentFile;

public interface DataRoomService {
    DataRoomResponse getDataRoomById(String roomId);
    DataRoom createDataRoom(String startupId, String investorId, String dealId);
    void signNda(String roomId);
    void upload(String roomId, String folder, MultipartFile file);
    void affectDocToDataRoom(String docId,String roomId);
    DocumentFile getDocument(String roomId, String documentId);
}
