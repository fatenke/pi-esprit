package tn.esprit.backend.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend.Services.NextBestActionService;
import tn.esprit.backend.Services.RequestActor;
import tn.esprit.backend.Services.UserRole;

import java.util.Map;

@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class DealNextBestActionController {
    private final NextBestActionService nextBestActionService;

    @PostMapping("/{requestId}/next-best-actions/generate-ai")
    public ResponseEntity<?> generateAiAction(
            @PathVariable String requestId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(nextBestActionService.generateAiAction(requestId, actor(userId, role)));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", safeMessage(e, "Access denied.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", safeMessage(e, "Invalid request.")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", safeMessage(e, "Unexpected server error while generating the AI recommendation.")));
        }
    }

    @GetMapping("/{requestId}/next-best-actions")
    public ResponseEntity<?> getActions(
            @PathVariable String requestId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(nextBestActionService.getActions(requestId, actor(userId, role)));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", safeMessage(e, "Access denied.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", safeMessage(e, "Invalid request.")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", safeMessage(e, "Unexpected server error while loading the actions.")));
        }
    }

    @PatchMapping("/next-best-actions/{actionId}/done")
    public ResponseEntity<?> markDone(
            @PathVariable String actionId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(nextBestActionService.markDone(actionId, actor(userId, role)));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", safeMessage(e, "Access denied.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", safeMessage(e, "Invalid request.")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", safeMessage(e, "Unexpected server error while updating the action.")));
        }
    }

    @PatchMapping("/next-best-actions/{actionId}/ignore")
    public ResponseEntity<?> ignore(
            @PathVariable String actionId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        try {
            return ResponseEntity.ok(nextBestActionService.ignore(actionId, actor(userId, role)));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", safeMessage(e, "Access denied.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", safeMessage(e, "Invalid request.")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", safeMessage(e, "Unexpected server error while updating the action.")));
        }
    }

    private RequestActor actor(String userId, String role) {
        String resolvedRole = role == null || role.isBlank() ? "INVESTOR" : role.trim().toUpperCase();
        if (resolvedRole.startsWith("ROLE_")) {
            resolvedRole = resolvedRole.substring(5);
        }
        String resolvedUser = userId == null ? "" : userId.trim();
        return new RequestActor(resolvedUser, UserRole.valueOf(resolvedRole));
    }

    private String safeMessage(Exception e, String fallback) {
        return e.getMessage() == null || e.getMessage().isBlank() ? fallback : e.getMessage();
    }
}
