package tn.esprit.backend.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend.DTO.DataRoomResponse;
import tn.esprit.backend.Entities.DataRoom;
import tn.esprit.backend.Entities.DocumentFile;
import tn.esprit.backend.Services.DataRoomService;
import tn.esprit.backend.Services.GridFsStorageService;

import java.io.IOException;

@RestController
@RequestMapping("/api/data-room")
@RequiredArgsConstructor
public class DataRoomController {
    private final DataRoomService dataRoomService;
    private final GridFsStorageService gridFsStorageService;

    @GetMapping("/{id}")
    public DataRoomResponse get(@PathVariable String id) {
        return dataRoomService.getDataRoomById(id);
    }

    @PostMapping("/add")
    public DataRoom createDR(@RequestBody DataRoom dR) {
        return dataRoomService.createDataRoom(dR.getStartupId(), dR.getInvestorId(), dR.getDealId());
    }

    @PostMapping("/deal/{dealId}/ensure")
    public DataRoom ensureForDeal(@PathVariable String dealId) {
        return dataRoomService.ensureDataRoomForDeal(dealId);
    }

    @PostMapping("/upload")
    public void upload(
            @RequestParam String roomId,
            @RequestParam String folder,
            @RequestParam MultipartFile file
    ) {
        dataRoomService.upload(roomId, folder, file);
    }

    @GetMapping("/{roomId}/documents/{documentId}/view")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable String roomId,
            @PathVariable String documentId
    ) throws IOException {
        return serveDocument(roomId, documentId, true);
    }

    @GetMapping("/{roomId}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable String roomId,
            @PathVariable String documentId
    ) throws IOException {
        return serveDocument(roomId, documentId, false);
    }

    private ResponseEntity<Resource> serveDocument(
            String roomId,
            String documentId,
            boolean inline
    ) throws IOException {
        DocumentFile document = dataRoomService.getDocument(roomId, documentId);
        if (document.getStorageId() == null || document.getStorageId().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        GridFsResource resource = gridFsStorageService.load(document.getStorageId());
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (document.getContentType() != null && !document.getContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(document.getContentType());
        }

        String disposition = inline ? "inline" : "attachment";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + document.getFileName() + "\""
                )
                .body(resource);
    }
}
