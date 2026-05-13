package tn.esprit.backend.Services;

import tn.esprit.backend.enums.DealStatus;
import tn.esprit.backend.Entities.InvestmentRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InvestmentRequestService {
    InvestmentRequest getInvestmentRequest(String irId);
    InvestmentRequest addInvestmentRequest(InvestmentRequest ir);

    /**
     * Création depuis le formulaire Angular (multipart) : introMessage, startupId, ticket optionnel, PDF optionnel.
     * {@code investorId} doit être fourni par la couche sécurité (JWT) ou un en-tête de dev.
     */
    InvestmentRequest addInvestmentRequestMultipart(
            String introMessage,
            String startupId,
            Long ticketProposed,
            MultipartFile investorDoc,
            String investorId
    );
    InvestmentRequest updateInvestmentRequest (InvestmentRequest ir);
    void deleteInvestmentRequest(String icId);
    List<InvestmentRequest> getAllInvestmentRequests();
    void syncStatus(String requestId, DealStatus dealStatus);
    List<InvestmentRequest> getInvestmentRequestByInvestor(String investorID);
    List<InvestmentRequest> getInvestmentRequestByStartup(String startupId);
    void acceptInvestmentRequest(String irId);
    void rejectInvestmentRequest(String irId);
}
