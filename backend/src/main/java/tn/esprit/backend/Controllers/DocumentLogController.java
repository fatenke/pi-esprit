package tn.esprit.backend.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/log")
public class DocumentLogController {

    @PostMapping("/view")
    public ResponseEntity<Map<String, Object>> logView(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of(
                "roomId", body.getOrDefault("roomId", ""),
                "documentId", body.getOrDefault("documentId", ""),
                "loggedAt", LocalDateTime.now().toString()
        ));
    }
}
