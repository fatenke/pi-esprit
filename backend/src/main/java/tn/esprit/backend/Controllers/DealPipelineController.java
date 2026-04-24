package tn.esprit.backend.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend.DTO.DealCard;
import tn.esprit.backend.DTO.KanbanBoard;
import tn.esprit.backend.DTO.MoveCardRequest;
import tn.esprit.backend.Entities.DealPipeline;
import tn.esprit.backend.Services.DealPipelineService;

@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class DealPipelineController {
    private final DealPipelineService dealPipelineService;

    @GetMapping("/board/{investorId}")
    public KanbanBoard getDealsByInvestor(@PathVariable String investorId) {
        return dealPipelineService.getBoard(investorId);
    }

    @GetMapping("/kanban/{investorId}")
    public KanbanBoard getKanban(@PathVariable String investorId) {
        return dealPipelineService.getBoard(investorId);
    }

    @PostMapping("/create-from-request")
    public DealCard createDealFromRequest(@RequestBody tn.esprit.backend.Entities.InvestmentRequest request) {
        return dealPipelineService.createDealFromRequest(request);
    }

    @PutMapping("/move")
    public ResponseEntity<DealCard> moveCard(@RequestBody MoveCardRequest req) {
        try {
            DealPipeline updated = dealPipelineService.moveCard(
                    req.getDealId(),
                    req.getNewStatus(),
                    req.getNewColumnOrder(),
                    "dev-investor"
            );

            return ResponseEntity.ok(dealPipelineService.toDealCard(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
