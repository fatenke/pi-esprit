package tn.esprit.backend.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend.DTO.CreateInvestmentHoldingRequest;
import tn.esprit.backend.DTO.CreateMilestoneRequest;
import tn.esprit.backend.DTO.InvestmentHoldingMilestoneResponse;
import tn.esprit.backend.DTO.InvestmentHoldingResponse;
import tn.esprit.backend.DTO.StripeConfigResponse;
import tn.esprit.backend.Services.*;
import tn.esprit.backend.config.StripeProperties;

import java.util.Map;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentHoldingController {
    private final InvestmentHoldingService investmentHoldingService;
    private final StripeProperties stripeProperties;

    @PostMapping("/{requestId}/holding")
    public ResponseEntity<?> createHolding(
            @PathVariable String requestId,
            @RequestBody CreateInvestmentHoldingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.createHolding(requestId, request, actor(userId, role)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", safeMessage(e, "Unexpected server error while creating the holding.")
            ));
        }
    }

    @GetMapping("/holding/{holdingId}")
    public ResponseEntity<?> getHolding(
            @PathVariable String holdingId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.getHolding(holdingId, actor(userId, role)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", safeMessage(e, "Unexpected server error while loading the holding.")
            ));
        }
    }

    @GetMapping("/request/{requestId}/holding")
    public ResponseEntity<?> getHoldingByRequest(
            @PathVariable String requestId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.getHoldingByRequestId(requestId, actor(userId, role)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", safeMessage(e, "Unexpected server error while loading the holding.")
            ));
        }
    }

    @PostMapping("/holding/{holdingId}/confirm-payment")
    public ResponseEntity<?> confirmPayment(
            @PathVariable String holdingId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.confirmPayment(holdingId, actor(userId, role)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", safeMessage(e, "Unexpected server error while confirming the payment.")
            ));
        }
    }

    @PostMapping("/holding/{holdingId}/request-release")
    public ResponseEntity<?> requestRelease(
            @PathVariable String holdingId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.requestRelease(holdingId, actor(userId, role)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", safeMessage(e, "Unexpected server error while requesting release.")
            ));
        }
    }

    @PostMapping("/holding/{holdingId}/dispute")
    public ResponseEntity<?> dispute(
            @PathVariable String holdingId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.dispute(holdingId, actor(userId, role)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", safeMessage(e, "Unexpected server error while opening the dispute.")
            ));
        }
    }

    @PostMapping("/holding/{holdingId}/milestones")
    public ResponseEntity<?> createMilestone(
            @PathVariable String holdingId,
            @RequestBody CreateMilestoneRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.createMilestone(holdingId, request, actor(userId, role)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", safeMessage(e, "Unexpected server error while creating the milestone.")
            ));
        }
    }

    @PatchMapping("/holding/milestones/{milestoneId}/validate")
    public ResponseEntity<?> validateMilestone(
            @PathVariable String milestoneId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.validateMilestone(milestoneId, actor(userId, role)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", safeMessage(e, "Unexpected server error while validating the milestone.")
            ));
        }
    }

    @GetMapping("/stripe/config")
    public ResponseEntity<?> stripeConfig() {
        if (stripeProperties.getPublicKey() == null || stripeProperties.getPublicKey().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Stripe publishable key is missing."));
        }
        return ResponseEntity.ok(new StripeConfigResponse(stripeProperties.getPublicKey()));
    }

    private RequestActor actor(String userId, String role) {
        String resolvedRole = role == null || role.isBlank() ? "INVESTOR" : role.trim().toUpperCase();
        String resolvedUser = userId == null ? "" : userId.trim();
        return new RequestActor(resolvedUser, UserRole.valueOf(resolvedRole));
    }

    private String safeMessage(Exception e, String fallback) {
        return e.getMessage() == null || e.getMessage().isBlank() ? fallback : e.getMessage();
    }
}
