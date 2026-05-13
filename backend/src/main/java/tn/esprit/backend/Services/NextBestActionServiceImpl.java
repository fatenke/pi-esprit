package tn.esprit.backend.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend.DTO.NextBestActionAiResult;
import tn.esprit.backend.DTO.NextBestActionResponse;
import tn.esprit.backend.Entities.DataRoom;
import tn.esprit.backend.Entities.DealPipeline;
import tn.esprit.backend.Entities.DocumentFile;
import tn.esprit.backend.Entities.InvestmentHolding;
import tn.esprit.backend.Entities.InvestmentRequest;
import tn.esprit.backend.Entities.NextBestAction;
import tn.esprit.backend.Repositories.DataRoomRepo;
import tn.esprit.backend.Repositories.DealPipelineRepo;
import tn.esprit.backend.Repositories.DocumentFileRepo;
import tn.esprit.backend.Repositories.InvestmentHoldingRepo;
import tn.esprit.backend.Repositories.NextBestActionRepo;
import tn.esprit.backend.enums.ActionStatus;
import tn.esprit.backend.enums.ActorRole;
import tn.esprit.backend.enums.DealStatus;
import tn.esprit.backend.enums.Priority;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NextBestActionServiceImpl implements NextBestActionService {
    private static final List<String> REQUIRED_DATA_ROOM_FOLDERS = List.of("FINANCIAL", "LEGAL", "PRODUCT", "TEAM");

    private final NextBestActionRepo nextBestActionRepo;
    private final InvestmentRequestService investmentRequestService;
    private final DealPipelineRepo dealPipelineRepo;
    private final DataRoomRepo dataRoomRepo;
    private final DocumentFileRepo documentFileRepo;
    private final InvestmentHoldingRepo investmentHoldingRepo;
    private final NextBestActionAIService nextBestActionAIService;

    @Override
    @Transactional
    public NextBestActionResponse generateAiAction(String requestId, RequestActor actor) {
        InvestmentRequest investmentRequest = investmentRequestService.getInvestmentRequest(requestId);
        DealPipeline deal = resolveDealForRequest(requestId);
        ensureRequestAccess(investmentRequest, actor);

        NextBestActionPromptContext context = buildContext(investmentRequest, deal);
        NextBestAction action;

        try {
            NextBestActionAiResult aiResult = nextBestActionAIService.generateAction(context);
            action = buildSavedAction(requestId, aiResult, true);
            log.info("Saved AI next best action for requestId={} with priority={}", requestId, action.getPriority());
        } catch (Exception e) {
            log.warn("OpenAI generation failed for requestId={}. Falling back to rule-based recommendation. reason={}",
                    requestId, e.getMessage());
            action = buildFallbackAction(requestId, context);
        }

        return toResponse(nextBestActionRepo.save(action));
    }

    @Override
    public List<NextBestActionResponse> getActions(String requestId, RequestActor actor) {
        InvestmentRequest investmentRequest = investmentRequestService.getInvestmentRequest(requestId);
        ensureRequestAccess(investmentRequest, actor);
        return nextBestActionRepo.findByInvestmentRequestIdOrderByCreatedAtDesc(requestId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NextBestActionResponse markDone(String actionId, RequestActor actor) {
        NextBestAction action = requireAction(actionId);
        InvestmentRequest investmentRequest = investmentRequestService.getInvestmentRequest(action.getInvestmentRequestId());
        ensureActionAccess(action, investmentRequest, actor);
        action.setStatus(ActionStatus.DONE);
        return toResponse(nextBestActionRepo.save(action));
    }

    @Override
    @Transactional
    public NextBestActionResponse ignore(String actionId, RequestActor actor) {
        NextBestAction action = requireAction(actionId);
        InvestmentRequest investmentRequest = investmentRequestService.getInvestmentRequest(action.getInvestmentRequestId());
        ensureActionAccess(action, investmentRequest, actor);
        action.setStatus(ActionStatus.IGNORED);
        return toResponse(nextBestActionRepo.save(action));
    }

    private NextBestActionPromptContext buildContext(InvestmentRequest investmentRequest, DealPipeline deal) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime statusChangedAt = deal.getStatusChangedAt() == null ? now : deal.getStatusChangedAt();
        long daysInStatus = Math.max(0, ChronoUnit.DAYS.between(statusChangedAt, now));

        Optional<DataRoom> dataRoomOpt = dataRoomRepo.findByDealId(deal.getId());
        List<DocumentFile> documents = dataRoomOpt.map(room -> documentFileRepo.findByRoomId(room.getId())).orElse(List.of());
        Set<String> presentFolders = new LinkedHashSet<>();
        for (DocumentFile document : documents) {
            if (document.getFolder() != null && !document.getFolder().isBlank()) {
                presentFolders.add(document.getFolder().trim().toUpperCase(Locale.ROOT));
            }
        }
        List<String> missingFolders = REQUIRED_DATA_ROOM_FOLDERS.stream()
                .filter(folder -> !presentFolders.contains(folder))
                .toList();

        InvestmentHolding holding = investmentHoldingRepo.findByInvestmentRequestId(investmentRequest.getId()).orElse(null);

        return NextBestActionPromptContext.builder()
                .investmentRequestId(investmentRequest.getId())
                .investorId(investmentRequest.getInvestorId())
                .startupId(investmentRequest.getStartupId())
                .investmentStatus(investmentRequest.getInvestmentStatus() == null ? null : investmentRequest.getInvestmentStatus().name())
                .ticketProposed(investmentRequest.getTicketProposed())
                .introMessage(investmentRequest.getIntroMessage())
                .dealStatus(deal.getStatus() == null ? null : deal.getStatus().name())
                .daysInStatus(daysInStatus)
                .statusChangedAt(statusChangedAt)
                .missingDataRoomFolders(missingFolders)
                .availableDataRoomDocuments(documents.size())
                .ndaSigned(dataRoomOpt.map(DataRoom::isNdaSigned).orElse(false))
                .holdingStatus(holding == null || holding.getStatus() == null ? null : holding.getStatus().name())
                .holdingAmountTnd(holding == null ? null : holding.getAmountTnd())
                .holdingAmountEur(holding == null ? null : holding.getAmountEur())
                .holdingFundedAt(holding == null ? null : holding.getFundedAt())
                .build();
    }

    private NextBestAction buildSavedAction(String requestId, NextBestActionAiResult aiResult, boolean aiGenerated) {
        LocalDateTime createdAt = LocalDateTime.now();
        int dueInDays = aiResult.getDueInDays() == null ? 1 : Math.max(0, aiResult.getDueInDays());

        return NextBestAction.builder()
                .investmentRequestId(requestId)
                .actorRole(parseActorRole(aiResult.getActorRole()))
                .title(requireText(aiResult.getTitle(), "AI title"))
                .description(requireText(aiResult.getDescription(), "AI description"))
                .priority(parsePriority(aiResult.getPriority()))
                .reason(requireText(aiResult.getReason(), "AI reason"))
                .status(ActionStatus.PENDING)
                .aiGenerated(aiGenerated)
                .createdAt(createdAt)
                .dueDate(createdAt.plusDays(dueInDays))
                .build();
    }

    private NextBestAction buildFallbackAction(String requestId, NextBestActionPromptContext context) {
        NextBestActionAiResult fallback;

        if (context.getHoldingStatus() != null && "WAITING_PAYMENT".equalsIgnoreCase(context.getHoldingStatus())) {
            fallback = NextBestActionAiResult.builder()
                    .actorRole("INVESTOR")
                    .title("Finaliser le paiement de test Stripe")
                    .description("Ouvrir le checkout Stripe test et confirmer le paiement pour faire passer le holding a FUNDS_HELD.")
                    .priority("URGENT")
                    .reason("Le holding existe deja mais le paiement n'est pas encore confirme.")
                    .dueInDays(1)
                    .build();
        } else if (context.getHoldingStatus() != null && "FUNDS_HELD".equalsIgnoreCase(context.getHoldingStatus())) {
            fallback = NextBestActionAiResult.builder()
                    .actorRole("STARTUP")
                    .title("Demander la liberation des fonds")
                    .description("Verifier les conditions du deal puis demander la liberation logique des fonds depuis le holding.")
                    .priority("HIGH")
                    .reason("Les fonds sont bloques et la prochaine etape operationnelle depend de la startup.")
                    .dueInDays(1)
                    .build();
        } else if (!context.getMissingDataRoomFolders().isEmpty()) {
            fallback = NextBestActionAiResult.builder()
                    .actorRole("STARTUP")
                    .title("Completer la data room")
                    .description("Ajouter les dossiers manquants de la data room pour fluidifier l'analyse du deal.")
                    .priority("HIGH")
                    .reason("Des documents manquent encore dans la data room: " + String.join(", ", context.getMissingDataRoomFolders()) + ".")
                    .dueInDays(2)
                    .build();
        } else if (context.getDaysInStatus() >= 7) {
            fallback = NextBestActionAiResult.builder()
                    .actorRole("INVESTOR")
                    .title("Relancer la startup sur l'etape en cours")
                    .description("Envoyer un message de suivi ou planifier un point rapide pour debloquer l'avancement du deal.")
                    .priority("HIGH")
                    .reason("Le deal est bloque dans le statut " + context.getDealStatus() + " depuis " + context.getDaysInStatus() + " jours.")
                    .dueInDays(1)
                    .build();
        } else {
            fallback = NextBestActionAiResult.builder()
                    .actorRole("INVESTOR")
                    .title("Clarifier la prochaine etape du deal")
                    .description("Verifier le dernier contexte du deal et proposer une action concrete a la startup dans les prochaines 24 heures.")
                    .priority("MEDIUM")
                    .reason("Aucune action critique automatique n'a ete detectee, mais un suivi operationnel reste utile.")
                    .dueInDays(2)
                    .build();
        }

        return buildSavedAction(requestId, fallback, false);
    }

    private DealPipeline resolveDealForRequest(String requestId) {
        return dealPipelineRepo.findAllByRequestId(requestId).stream()
                .max(Comparator.comparing(
                        DealPipeline::getStatusChangedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).thenComparing(DealPipeline::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new IllegalArgumentException("No deal pipeline exists for this investment request."));
    }

    private NextBestAction requireAction(String actionId) {
        return nextBestActionRepo.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Next best action not found."));
    }

    private void ensureRequestAccess(InvestmentRequest investmentRequest, RequestActor actor) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.INVESTOR && investmentRequest.getInvestorId().equals(actor.getUserId())) {
            return;
        }
        if (actor.getRole() == UserRole.STARTUP && investmentRequest.getStartupId().equals(actor.getUserId())) {
            return;
        }
        throw new SecurityException("You are not allowed to access next best actions for this deal.");
    }

    private void ensureActionAccess(NextBestAction action, InvestmentRequest investmentRequest, RequestActor actor) {
        ensureRequestAccess(investmentRequest, actor);
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        ActorRole expectedActorRole = mapRole(actor.getRole());
        if (action.getActorRole() != expectedActorRole) {
            throw new SecurityException("Only the targeted role or an admin can update this action.");
        }
    }

    private ActorRole mapRole(UserRole role) {
        return ActorRole.valueOf(role.name());
    }

    private ActorRole parseActorRole(String value) {
        try {
            return ActorRole.valueOf(requireText(value, "AI actorRole").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("AI actorRole is invalid.");
        }
    }

    private Priority parsePriority(String value) {
        try {
            return Priority.valueOf(requireText(value, "AI priority").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("AI priority is invalid.");
        }
    }

    private String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private NextBestActionResponse toResponse(NextBestAction action) {
        return NextBestActionResponse.builder()
                .id(action.getId())
                .investmentRequestId(action.getInvestmentRequestId())
                .actorRole(action.getActorRole())
                .title(action.getTitle())
                .description(action.getDescription())
                .priority(action.getPriority())
                .reason(action.getReason())
                .status(action.getStatus())
                .aiGenerated(action.getAiGenerated())
                .createdAt(action.getCreatedAt())
                .dueDate(action.getDueDate())
                .build();
    }
}
