package tn.esprit.backend.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend.Services.DataRoomService;

import java.util.Map;

@RestController
@RequestMapping("/api/nda")
@RequiredArgsConstructor
public class NdaController {
    private final DataRoomService dataRoomService;

    @PostMapping("/sign")
    public ResponseEntity<Map<String, Object>> sign(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId");
        if (roomId == null || roomId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "roomId is required"));
        }

        dataRoomService.signNda(roomId);
        return ResponseEntity.ok(Map.of("roomId", roomId, "signed", true));
    }
}
