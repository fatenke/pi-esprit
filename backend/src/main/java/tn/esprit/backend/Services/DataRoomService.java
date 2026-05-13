package tn.esprit.backend.Services;

import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend.DTO.DataRoomResponse;
import tn.esprit.backend.Entities.DataRoom;
import tn.esprit.backend.Entities.DocumentFile;

public interface DataRoomService {
    DataRoomResponse getDataRoomById(String roomId, RequestActor actor);
    DataRoom createDataRoom(String startupId, String investorId, String dealId);
    DataRoom ensureDataRoomForDeal(String dealId);
    void upload(String roomId, String folder, MultipartFile file, RequestActor actor);
    void affectDocToDataRoom(String docId,String roomId);
    DocumentFile getDocument(String roomId, String documentId, RequestActor actor);
}
