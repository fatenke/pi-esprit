package tn.esprit.backend.Controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend.DTO.NdaAgreementResponse;
import tn.esprit.backend.DTO.NdaSignResponse;
import tn.esprit.backend.DTO.NdaStatusResponse;
import tn.esprit.backend.DTO.SignNdaRequest;
import tn.esprit.backend.Entities.NdaAgreement;
import tn.esprit.backend.Entities.NdaSignature;
import tn.esprit.backend.Services.NdaService;
import tn.esprit.backend.Services.RequestActor;
import tn.esprit.backend.Services.UserRole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/dataroom/{dataRoomId}/nda")
@RequiredArgsConstructor
public class DataRoomNdaController {
    private final NdaService ndaService;

    @PostMapping("/create")
    public ResponseEntity<?> create(
            @PathVariable String dataRoomId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            ndaService.createOrGetNda(dataRoomId);
            return ResponseEntity.ok(ndaService.getNda(dataRoomId, actor(userId, role)));
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> get(
            @PathVariable String dataRoomId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(ndaService.getNda(dataRoomId, actor(userId, role)));
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/sign")
    public ResponseEntity<?> sign(
            @PathVariable String dataRoomId,
            @RequestBody SignNdaRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            HttpServletRequest servletRequest
    ) {
        try {
            NdaSignResponse response = ndaService.signNda(
                    dataRoomId,
                    request,
                    actor(userId, role),
                    servletRequest.getRemoteAddr(),
                    servletRequest.getHeader("User-Agent")
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/certificate")
    public ResponseEntity<?> getCertificate(
            @PathVariable String dataRoomId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            NdaSignature signature = ndaService.getSignatureForCertificate(dataRoomId, actor(userId, role));
            NdaAgreementResponse agreement = ndaService.getNda(dataRoomId, actor(userId, role));

            String content = """
                    CERTIFICAT DE SIGNATURE NDA

                    Nom du signataire: %s
                    Type de signature: %s
                    ID Data Room: %s
                    ID Investisseur: %s
                    ID Startup: %s
                    Date signature: %s
                    NDA Hash: %s
                    Signature Hash: %s
                    """.formatted(
                    signature.getSignerFullName(),
                    signature.getSignatureType(),
                    dataRoomId,
                    signature.getSignerId(),
                    ndaService.createOrGetNda(dataRoomId).getStartupId(),
                    signature.getSignedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    agreement.getNdaHash(),
                    signature.getSignatureHash()
            );

            ByteArrayResource resource = new ByteArrayResource(content.getBytes());
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nda-certificate-%s.txt\"".formatted(dataRoomId))
                    .body(resource);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(
            @PathVariable String dataRoomId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            NdaStatusResponse response = ndaService.getNdaStatus(dataRoomId, actor(userId, role));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/signatures/{signatureId}/image")
    public ResponseEntity<?> getSignatureImage(
            @PathVariable String dataRoomId,
            @PathVariable String signatureId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            ndaService.getSignatureForCertificate(dataRoomId, actor(userId, role));
            Path file = Path.of("storage", "nda-signatures", "%s-%s.png".formatted(dataRoomId, signatureId));
            if (!Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(file));
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(resource);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to read the signature image."));
        }
    }

    private NdaAgreementResponse toAgreementResponse(NdaAgreement agreement) {
        return NdaAgreementResponse.builder()
                .id(agreement.getId())
                .dataRoomId(agreement.getDataRoomId())
                .status(agreement.getStatus())
                .ndaContent(agreement.getNdaContent())
                .ndaHash(agreement.getNdaHash())
                .signedAt(agreement.getSignedAt())
                .build();
    }

    private RequestActor actor(String userId, String role) {
        String resolvedRole = role == null || role.isBlank() ? "INVESTOR" : role.trim().toUpperCase();
        String resolvedUser = userId == null || userId.isBlank() ? "dev-investor" : userId.trim();
        return new RequestActor(resolvedUser, UserRole.valueOf(resolvedRole));
    }
}
