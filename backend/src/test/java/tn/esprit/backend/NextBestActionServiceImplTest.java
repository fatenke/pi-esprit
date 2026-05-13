package tn.esprit.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend.DTO.NextBestActionAiResult;
import tn.esprit.backend.Entities.DataRoom;
import tn.esprit.backend.Entities.DealPipeline;
import tn.esprit.backend.Entities.DocumentFile;
import tn.esprit.backend.Entities.InvestmentRequest;
import tn.esprit.backend.Entities.NextBestAction;
import tn.esprit.backend.Repositories.DataRoomRepo;
import tn.esprit.backend.Repositories.DealPipelineRepo;
import tn.esprit.backend.Repositories.DocumentFileRepo;
import tn.esprit.backend.Repositories.InvestmentHoldingRepo;
import tn.esprit.backend.Repositories.NextBestActionRepo;
import tn.esprit.backend.Services.InvestmentRequestService;
import tn.esprit.backend.Services.NextBestActionAIService;
import tn.esprit.backend.Services.NextBestActionServiceImpl;
import tn.esprit.backend.Services.RequestActor;
import tn.esprit.backend.Services.UserRole;
import tn.esprit.backend.enums.ActionStatus;
import tn.esprit.backend.enums.ActorRole;
import tn.esprit.backend.enums.DealStatus;
import tn.esprit.backend.enums.InvestmentStatus;
import tn.esprit.backend.enums.Priority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NextBestActionServiceImplTest {

    @Mock
    private NextBestActionRepo nextBestActionRepo;
    @Mock
    private InvestmentRequestService investmentRequestService;
    @Mock
    private DealPipelineRepo dealPipelineRepo;
    @Mock
    private DataRoomRepo dataRoomRepo;
    @Mock
    private DocumentFileRepo documentFileRepo;
    @Mock
    private InvestmentHoldingRepo investmentHoldingRepo;
    @Mock
    private NextBestActionAIService nextBestActionAIService;

    @InjectMocks
    private NextBestActionServiceImpl service;

    private InvestmentRequest request;
    private DealPipeline deal;

    @BeforeEach
    void setUp() {
        request = new InvestmentRequest();
        request.setId("req-1");
        request.setInvestorId("dev-investor");
        request.setStartupId("s-001");
        request.setInvestmentStatus(InvestmentStatus.PENDING);
        request.setTicketProposed(50000L);
        request.setIntroMessage("We need updated KPI metrics.");

        deal = new DealPipeline();
        deal.setId("deal-1");
        deal.setRequestId("req-1");
        deal.setInvestorId("dev-investor");
        deal.setStartupId("s-001");
        deal.setStatus(DealStatus.NEGOTIATION);
        deal.setStatusChangedAt(LocalDateTime.now().minusDays(5));
    }

    @Test
    void shouldGenerateAndSaveAiAction() {
        when(investmentRequestService.getInvestmentRequest("req-1")).thenReturn(request);
        when(dealPipelineRepo.findAllByRequestId("req-1")).thenReturn(List.of(deal));
        when(dataRoomRepo.findByDealId("deal-1")).thenReturn(Optional.empty());
        when(investmentHoldingRepo.findByInvestmentRequestId("req-1")).thenReturn(Optional.empty());
        when(nextBestActionAIService.generateAction(any())).thenReturn(
                NextBestActionAiResult.builder()
                        .actorRole("INVESTOR")
                        .title("Programmer une relance")
                        .description("Envoyer un message clair avec les prochaines pieces attendues.")
                        .priority("HIGH")
                        .reason("Le deal attend une action de suivi cote investisseur.")
                        .dueInDays(2)
                        .build()
        );
        when(nextBestActionRepo.save(any(NextBestAction.class))).thenAnswer(invocation -> {
            NextBestAction action = invocation.getArgument(0);
            action.setId("nba-1");
            return action;
        });

        var response = service.generateAiAction("req-1", new RequestActor("dev-investor", UserRole.INVESTOR));

        assertEquals("nba-1", response.getId());
        assertEquals(ActorRole.INVESTOR, response.getActorRole());
        assertEquals(Priority.HIGH, response.getPriority());
        assertEquals(ActionStatus.PENDING, response.getStatus());
        assertEquals(Boolean.TRUE, response.getAiGenerated());
        assertNotNull(response.getDueDate());
    }

    @Test
    void shouldFallbackWhenAiCallFails() {
        DataRoom room = DataRoom.builder()
                .id("room-1")
                .dealId("deal-1")
                .ndaSigned(true)
                .build();

        when(investmentRequestService.getInvestmentRequest("req-1")).thenReturn(request);
        when(dealPipelineRepo.findAllByRequestId("req-1")).thenReturn(List.of(deal));
        when(dataRoomRepo.findByDealId("deal-1")).thenReturn(Optional.of(room));
        when(documentFileRepo.findByRoomId("room-1")).thenReturn(List.of(
                DocumentFile.builder().id("doc-1").roomId("room-1").folder("FINANCIAL").build()
        ));
        when(investmentHoldingRepo.findByInvestmentRequestId("req-1")).thenReturn(Optional.empty());
        when(nextBestActionAIService.generateAction(any())).thenThrow(new IllegalStateException("rate limit"));
        when(nextBestActionRepo.save(any(NextBestAction.class))).thenAnswer(invocation -> {
            NextBestAction action = invocation.getArgument(0);
            action.setId("nba-2");
            return action;
        });

        var response = service.generateAiAction("req-1", new RequestActor("dev-investor", UserRole.INVESTOR));

        assertEquals("nba-2", response.getId());
        assertEquals(ActorRole.STARTUP, response.getActorRole());
        assertEquals(Priority.HIGH, response.getPriority());
        assertFalse(response.getAiGenerated());
        assertEquals(ActionStatus.PENDING, response.getStatus());
    }

    @Test
    void shouldRefuseMarkDoneForWrongRole() {
        NextBestAction action = NextBestAction.builder()
                .id("nba-3")
                .investmentRequestId("req-1")
                .actorRole(ActorRole.STARTUP)
                .title("Completer la data room")
                .description("...")
                .priority(Priority.HIGH)
                .reason("...")
                .status(ActionStatus.PENDING)
                .aiGenerated(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(nextBestActionRepo.findById("nba-3")).thenReturn(Optional.of(action));
        when(investmentRequestService.getInvestmentRequest("req-1")).thenReturn(request);

        assertThrows(
                SecurityException.class,
                () -> service.markDone("nba-3", new RequestActor("dev-investor", UserRole.INVESTOR))
        );
    }
}
