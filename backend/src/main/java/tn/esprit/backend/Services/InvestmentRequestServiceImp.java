package tn.esprit.backend.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend.Entities.DealPipeline;
import tn.esprit.backend.enums.DealStatus;
import tn.esprit.backend.Entities.InvestmentRequest;
import tn.esprit.backend.enums.InvestmentStatus;
import tn.esprit.backend.Repositories.DealPipelineRepo;
import tn.esprit.backend.Repositories.InvestmentRequestRepo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentRequestServiceImp implements InvestmentRequestService {
    private final InvestmentRequestRepo investmentRequestRepo;
    private final DealPipelineRepo dealPipelineRepo;
    private final GridFsStorageService gridFsStorageService;

    @Override
    public InvestmentRequest getInvestmentRequest(String irId) {
        return investmentRequestRepo.findById(irId)
                .orElseThrow(() -> new IllegalArgumentException("Investment request not found."));
    }

    @Override
    public InvestmentRequest addInvestmentRequest(InvestmentRequest ir) {
        return investmentRequestRepo.save(ir);
    }

    @Override
    public InvestmentRequest addInvestmentRequestMultipart(
            String introMessage,
            String startupId,
            Long ticketProposed,
            MultipartFile investorDoc,
            String investorId
    ) {
        String msg = introMessage == null ? "" : introMessage.trim();
        if (msg.length() < 20 || msg.length() > 500) {
            throw new IllegalArgumentException("Le message doit contenir entre 20 et 500 caracteres.");
        }
        if (startupId == null || startupId.isBlank()) {
            throw new IllegalArgumentException("startupId est obligatoire.");
        }
        if (investorId == null || investorId.isBlank()) {
            throw new IllegalArgumentException("investorId manquant (JWT ou en-tete X-Investor-Id).");
        }

        String docUrl = null;
        if (investorDoc != null && !investorDoc.isEmpty()) {
            if (investorDoc.getSize() > 5L * 1024 * 1024) {
                throw new IllegalArgumentException("Le document ne doit pas depasser 5 Mo.");
            }
            String ct = investorDoc.getContentType();
            String name = investorDoc.getOriginalFilename();
            boolean looksPdf = "application/pdf".equalsIgnoreCase(ct)
                    || (name != null && name.toLowerCase().endsWith(".pdf"));
            if (!looksPdf) {
                throw new IllegalArgumentException("Seuls les fichiers PDF sont acceptes.");
            }
            try {
                docUrl = gridFsStorageService.storeInvestorPdf(investorDoc, startupId.trim());
            } catch (IOException e) {
                throw new RuntimeException("Echec du stockage du document.", e);
            }
        }

        InvestmentRequest ir = new InvestmentRequest();
        ir.setInvestorId(investorId.trim());
        ir.setStartupId(startupId.trim());
        ir.setIntroMessage(msg);
        ir.setTicketProposed(ticketProposed);
        ir.setInvestorDocUrl(docUrl);
        ir.setInvestmentStatus(InvestmentStatus.PENDING);
        ir.setSentAt(LocalDateTime.now());
        return investmentRequestRepo.save(ir);
    }

    @Override
    public InvestmentRequest updateInvestmentRequest(InvestmentRequest ir) {
        return investmentRequestRepo.save(ir);
    }

    @Override
    public void deleteInvestmentRequest(String icId) {
        investmentRequestRepo.deleteById(icId);
    }

    @Override
    public List<InvestmentRequest> getAllInvestmentRequests() {
        return investmentRequestRepo.findAll();
    }

    @Override
    public void syncStatus(String requestId, DealStatus dealStatus) {
        InvestmentRequest request = investmentRequestRepo.findById(requestId).orElseThrow();

        switch (dealStatus) {
            case DISCOVERY:
            case CONTACTED:
                request.setInvestmentStatus(InvestmentStatus.PENDING);
                break;
            case NEGOTIATION:
            case DUE_DILIGENCE:
            case VERIFICATION_FINALE:
            case ACCEPTED:
                request.setInvestmentStatus(InvestmentStatus.ACCEPTED);
                break;
            case REJECTED:
                request.setInvestmentStatus(InvestmentStatus.REJECTED);
                break;
            case CLOSED:
                request.setInvestmentStatus(InvestmentStatus.COMPLETED);
                break;
        }

        investmentRequestRepo.save(request);
    }

    @Override
    public List<InvestmentRequest> getInvestmentRequestByInvestor(String investorID) {
        return investmentRequestRepo.findByInvestorId(investorID);
    }

    @Override
    public List<InvestmentRequest> getInvestmentRequestByStartup(String startupId) {
        return investmentRequestRepo.findByStartupId(startupId);
    }

    @Override
    public void acceptInvestmentRequest(String irId) {
        InvestmentRequest req = investmentRequestRepo.findById(irId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (req.getInvestmentStatus() != InvestmentStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }

        req.setInvestmentStatus(InvestmentStatus.ACCEPTED);
        investmentRequestRepo.save(req);

        boolean dealAlreadyExists = dealPipelineRepo.findAll().stream()
                .anyMatch(deal -> req.getId().equals(deal.getRequestId()));

        if (!dealAlreadyExists) {
            DealPipeline deal = new DealPipeline();
            deal.setInvestorId(req.getInvestorId());
            deal.setStartupId(req.getStartupId());
            deal.setRequestId(req.getId());
            deal.setStatus(DealStatus.DISCOVERY);
            deal.setColumnOrder(0);
            deal.setStatusChangedAt(LocalDateTime.now());
            dealPipelineRepo.save(deal);
        }
    }

    @Override
    public void rejectInvestmentRequest(String irId) {
        InvestmentRequest req = investmentRequestRepo.findById(irId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (req.getInvestmentStatus() != InvestmentStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }
        req.setInvestmentStatus(InvestmentStatus.REJECTED);
        investmentRequestRepo.save(req);
    }
}
