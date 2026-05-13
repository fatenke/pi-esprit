package tn.esprit.backend.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend.DTO.DealCard;
import tn.esprit.backend.DTO.KanbanBoard;
import tn.esprit.backend.Entities.DealPipeline;
import tn.esprit.backend.enums.DealStatus;
import tn.esprit.backend.Entities.StatusHistory;
import tn.esprit.backend.Repositories.DealPipelineRepo;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DealPipelineService {
    private final DealPipelineRepo dealPipelineRepo;
    private final InvestmentRequestService investmentRequestService;
    private static final Map<DealStatus, Integer> STATUS_STAGE = new EnumMap<>(DealStatus.class);

    static {
        STATUS_STAGE.put(DealStatus.DISCOVERY, 0);
        STATUS_STAGE.put(DealStatus.CONTACTED, 1);
        STATUS_STAGE.put(DealStatus.NEGOTIATION, 2);
        STATUS_STAGE.put(DealStatus.DUE_DILIGENCE, 3);
        STATUS_STAGE.put(DealStatus.VERIFICATION_FINALE, 3);
        STATUS_STAGE.put(DealStatus.CLOSED, 4);
        STATUS_STAGE.put(DealStatus.REJECTED, 4);
        STATUS_STAGE.put(DealStatus.ACCEPTED, 1);
    }

    public DealCard toDealCard(DealPipeline deal) {
        Long ticketProposed = null;
        if (deal.getRequestId() != null && !deal.getRequestId().isBlank()) {
            try {
                ticketProposed = investmentRequestService
                        .getInvestmentRequest(deal.getRequestId())
                        .getTicketProposed();
            } catch (RuntimeException ignored) {
                ticketProposed = null;
            }
        }

        DealCard card = new DealCard();
        card.setId(deal.getId());
        card.setStartupId(deal.getStartupId());
        card.setRequestId(deal.getRequestId());
        card.setStatus(normalizeDisplayStatus(deal.getStatus()).name());
        card.setColumnOrder(deal.getColumnOrder());
        card.setTicketProposed(ticketProposed);
        card.setLastStatusChangeAt(deal.getStatusChangedAt());
        card.setAlertLevel("NONE");

        long days = ChronoUnit.DAYS.between(
                deal.getStatusChangedAt(),
                LocalDateTime.now()
        );
        card.setDaysInStatus(days);

        return card;
    }

    public KanbanBoard getBoard(String investorId) {
        List<DealPipeline> deals = dealPipelineRepo.findByInvestorIdOrderByColumnOrderAsc(investorId);

        Map<DealStatus, List<DealCard>> columns = deals.stream()
                .collect(Collectors.groupingBy(
                        deal -> normalizeDisplayStatus(deal.getStatus()),
                        Collectors.mapping(this::toDealCard, Collectors.toList())
                ));

        return KanbanBoard.builder()
                .columns(columns)
                .totalDeals(deals.size())
                .build();
    }

    @Transactional
    public DealCard createDealFromRequest(tn.esprit.backend.Entities.InvestmentRequest request) {
        DealPipeline deal = new DealPipeline();
        deal.setInvestorId(request.getInvestorId());
        deal.setStartupId(request.getStartupId());
        deal.setRequestId(request.getId());
        deal.setStatus(DealStatus.DISCOVERY);
        deal.setColumnOrder(0);
        deal.setStatusChangedAt(LocalDateTime.now());

        DealPipeline savedDeal = dealPipelineRepo.save(deal);
        return toDealCard(savedDeal);
    }

    @Transactional
    public DealPipeline moveCard(String dealId, DealStatus newStatus, int newOrder, String userId) {
        DealPipeline deal = dealPipelineRepo.findById(dealId).orElseThrow();
        DealStatus oldStatus = deal.getStatus();

        if (oldStatus == DealStatus.CLOSED || oldStatus == DealStatus.REJECTED) {
            throw new IllegalArgumentException("Terminal deals cannot change status");
        }

        if (oldStatus == DealStatus.CLOSED && newStatus == DealStatus.REJECTED) {
            throw new IllegalArgumentException("A closed deal cannot be rejected");
        }

        int oldStage = STATUS_STAGE.getOrDefault(oldStatus, 0);
        int newStage = STATUS_STAGE.getOrDefault(newStatus, 0);
        if (newStage < oldStage) {
            throw new IllegalArgumentException("Backward status moves are not allowed");
        }

        if (deal.getHistory() == null) {
            deal.setHistory(new ArrayList<>());
        }

        deal.getHistory().add(StatusHistory.builder()
                .fromStatus(oldStatus)
                .toStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .changedBy(userId)
                .build());

        deal.setStatus(newStatus);
        deal.setColumnOrder(newOrder);
        deal.setStatusChangedAt(LocalDateTime.now());

        investmentRequestService.syncStatus(deal.getRequestId(), newStatus);

        return dealPipelineRepo.save(deal);
    }

    private DealStatus normalizeDisplayStatus(DealStatus status) {
        if (status == DealStatus.VERIFICATION_FINALE) {
            return DealStatus.DUE_DILIGENCE;
        }
        return status;
    }
}
