package tn.esprit.backend.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend.Entities.InvestmentRequest;
import tn.esprit.backend.Services.GridFsStorageService;
import tn.esprit.backend.Services.InvestmentRequestService;
import org.springframework.data.mongodb.gridfs.GridFsResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/invest-request")
@RequiredArgsConstructor
public class InvestmentRequestController {
    private final InvestmentRequestService investmentRequestService;
    private final GridFsStorageService gridFsStorageService;

    /**
     * Création d’une demande (formulaire Angular en multipart).
     * Champs : introMessage, startupId, ticketProposed (optionnel), investorDoc (optionnel, PDF).
     * {@code investorId} : en production depuis le JWT ; en local possible via en-tête {@code X-Investor-Id}.
     */
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addIrMultipart(
            @RequestParam("introMessage") String introMessage,
            @RequestParam("startupId") String startupId,
            @RequestParam(value = "ticketProposed", required = false) Long ticketProposed,
            @RequestParam(value = "investorDoc", required = false) MultipartFile investorDoc,
            @RequestHeader(value = "X-Investor-Id", required = false) String investorIdHeader
    ) {
        String investorId = (investorIdHeader != null && !investorIdHeader.isBlank())
                ? investorIdHeader.trim()
                : "dev-investor";
        try {
            InvestmentRequest saved = investmentRequestService.addInvestmentRequestMultipart(
                    introMessage,
                    startupId,
                    ticketProposed,
                    investorDoc,
                    investorId
            );
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Téléchargement / consultation d’un PDF stocké dans GridFS (URL retournée dans {@code investorDocUrl}). */
    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> getInvestorDoc(@PathVariable String id) throws IOException {
        GridFsResource resource = gridFsStorageService.load(id);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String filename = resource.getFilename() != null ? resource.getFilename() : "document.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PutMapping("/{irId}/update")
    public InvestmentRequest updateIr(@RequestBody InvestmentRequest ir) {
        return investmentRequestService.updateInvestmentRequest(ir);
    }

    @GetMapping("/get/{irId}")
    public InvestmentRequest getIr(@PathVariable String irId) {
        return investmentRequestService.getInvestmentRequest(irId);
    }

    @GetMapping("/getAll")
    public List<InvestmentRequest> getAllIr() {
        return investmentRequestService.getAllInvestmentRequests();
    }

    @DeleteMapping("/remove/{irId}")
    public void deleteIr(@PathVariable String irId) {
        investmentRequestService.deleteInvestmentRequest(irId);
    }

    @GetMapping("/investor/{investorId}")
    public List<InvestmentRequest> getIrByInvestor(@PathVariable String investorId) {
        return investmentRequestService.getInvestmentRequestByInvestor(investorId);
    }

    @GetMapping("/startup/{startupId}")
    public List<InvestmentRequest> getIrByStartup(@PathVariable String startupId) {
        return investmentRequestService.getInvestmentRequestByStartup(startupId);
    }

    @PutMapping("/{irId}/accept")
    public void accept(@PathVariable String irId) {
        investmentRequestService.acceptInvestmentRequest(irId);
    }
    @PutMapping("/{irId}/reject")
    public void reject(@PathVariable String irId) {
        investmentRequestService.rejectInvestmentRequest(irId);
    }

}

