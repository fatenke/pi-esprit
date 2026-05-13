package tn.esprit.backend.Services;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend.DTO.DataRoomResponse;
import tn.esprit.backend.Entities.DataRoom;
import tn.esprit.backend.Entities.DealPipeline;
import tn.esprit.backend.Entities.DocumentFile;
import tn.esprit.backend.enums.NdaStatus;
import tn.esprit.backend.Repositories.DataRoomRepo;
import tn.esprit.backend.Repositories.DealPipelineRepo;
import tn.esprit.backend.Repositories.DocumentFileRepo;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataRoomServiceImp implements DataRoomService {
    private final DataRoomRepo dataRoomRepo;
    private final DocumentFileRepo documentFileRepo;
    private final DealPipelineRepo dealPipelineRepo;
    private final GridFsStorageService gridFsStorageService;
    private final NdaService ndaService;

    public DataRoom createDataRoom(String startupId, String investorId, String dealId) {
        DataRoom room = new DataRoom();
        room.setStartupId(startupId);
        room.setInvestorId(investorId);
        room.setDealId(dealId);
        room.setNdaSigned(false);
        room.setStatus("ACTIVE");
        room.setCreatedAt(LocalDateTime.now());
        return dataRoomRepo.save(room);
    }

    @Override
    public DataRoom ensureDataRoomForDeal(String dealId) {
        if (dealId == null || dealId.isBlank()) {
            throw new IllegalArgumentException("dealId is required");
        }

        return dataRoomRepo.findByDealId(dealId)
                .orElseGet(() -> {
                    DealPipeline deal = dealPipelineRepo.findById(dealId)
                            .orElseThrow(() -> new RuntimeException("Deal not found"));
                    return createDataRoom(deal.getStartupId(), deal.getInvestorId(), deal.getId());
                });
    }

    @Override
    public void affectDocToDataRoom(String docId, String roomId) {
        DocumentFile doc = documentFileRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        dataRoomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("DataRoom not found"));
        doc.setRoomId(roomId);
        documentFileRepo.save(doc);
    }

    @Override
    public DataRoomResponse getDataRoomById(String roomId, RequestActor actor) {
        DataRoom room = dataRoomRepo.findById(roomId)
                .orElseThrow();

        boolean canAccessDocuments = ndaService.canAccessDataRoom(roomId, actor);
        List<DocumentFile> docs = canAccessDocuments ? documentFileRepo.findByRoomId(roomId) : List.of();

        return DataRoomResponse.builder()
                .roomId(room.getId())
                .ndaSigned(ndaService.createOrGetNda(roomId).getStatus() == NdaStatus.SIGNED)
                .documents(docs)
                .build();
    }

    @Override
    public void upload(String roomId, String folder, MultipartFile file, RequestActor actor) {
        if (!ndaService.canAccessDataRoom(roomId, actor)) {
            throw new SecurityException("You must sign the NDA before accessing data room documents.");
        }
        dataRoomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Data room not found"));

        String storageId;
        try {
            storageId = gridFsStorageService.store(
                    file,
                    "data-room-document",
                    new Document("roomId", roomId).append("folder", folder)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to store document", e);
        }

        DocumentFile doc = new DocumentFile();
        doc.setRoomId(roomId);
        doc.setFileName(file.getOriginalFilename());
        doc.setFolder(folder);
        doc.setStorageId(storageId);
        doc.setContentType(file.getContentType());
        doc.setType(file.getContentType());
        doc.setCreatedAt(LocalDateTime.now());

        documentFileRepo.save(doc);
    }

    @Override
    public DocumentFile getDocument(String roomId, String documentId, RequestActor actor) {
        if (!ndaService.canAccessDataRoom(roomId, actor)) {
            throw new SecurityException("You must sign the NDA before accessing data room documents.");
        }
        dataRoomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Data room not found"));

        DocumentFile document = documentFileRepo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!roomId.equals(document.getRoomId())) {
            throw new RuntimeException("Document does not belong to this data room");
        }

        return document;
    }
}
