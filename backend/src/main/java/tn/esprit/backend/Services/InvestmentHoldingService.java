package tn.esprit.backend.Services;

import tn.esprit.backend.DTO.CheckoutSessionResponse;
import tn.esprit.backend.DTO.ConfirmCheckoutRequest;
import tn.esprit.backend.DTO.CreateCheckoutSessionRequest;
import tn.esprit.backend.DTO.CreateMilestoneRequest;
import tn.esprit.backend.DTO.InvestmentHoldingMilestoneResponse;
import tn.esprit.backend.DTO.InvestmentHoldingResponse;

public interface InvestmentHoldingService {
    CheckoutSessionResponse createCheckoutSession(String requestId, CreateCheckoutSessionRequest request, RequestActor actor);
    InvestmentHoldingResponse confirmCheckout(ConfirmCheckoutRequest request);
    InvestmentHoldingResponse getHolding(String holdingId, RequestActor actor);
    InvestmentHoldingResponse getHoldingByRequestId(String requestId, RequestActor actor);
    InvestmentHoldingResponse requestRelease(String holdingId, RequestActor actor);
    InvestmentHoldingResponse releaseFunds(String holdingId, RequestActor actor);
    InvestmentHoldingResponse dispute(String holdingId, RequestActor actor);
    InvestmentHoldingResponse refund(String holdingId, RequestActor actor);
    InvestmentHoldingMilestoneResponse createMilestone(String holdingId, CreateMilestoneRequest request, RequestActor actor);
    InvestmentHoldingMilestoneResponse validateMilestone(String milestoneId, RequestActor actor);
    InvestmentHoldingMilestoneResponse releaseMilestone(String milestoneId, RequestActor actor);
}
