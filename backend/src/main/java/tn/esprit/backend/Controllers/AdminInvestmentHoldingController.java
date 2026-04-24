package tn.esprit.backend.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend.Services.InvestmentHoldingService;
import tn.esprit.backend.Services.RequestActor;
import tn.esprit.backend.Services.UserRole;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/investments/holding")
@RequiredArgsConstructor
public class AdminInvestmentHoldingController {
    private final InvestmentHoldingService investmentHoldingService;

    @PostMapping("/{holdingId}/release")
    public ResponseEntity<?> release(
            @PathVariable String holdingId,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.releaseFunds(holdingId, admin(userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{holdingId}/refund")
    public ResponseEntity<?> refund(
            @PathVariable String holdingId,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.refund(holdingId, admin(userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/milestones/{milestoneId}/release")
    public ResponseEntity<?> releaseMilestone(
            @PathVariable String milestoneId,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        try {
            return ResponseEntity.ok(investmentHoldingService.releaseMilestone(milestoneId, admin(userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private RequestActor admin(String userId) {
        return new RequestActor(userId == null ? "admin-user" : userId.trim(), UserRole.ADMIN);
    }
}
