package tn.esprit.backend.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend.DTO.CheckoutSessionResponse;
import tn.esprit.backend.DTO.ConfirmCheckoutRequest;
import tn.esprit.backend.DTO.CreateCheckoutSessionRequest;
import tn.esprit.backend.DTO.CreateMilestoneRequest;
import tn.esprit.backend.DTO.InvestmentHoldingMilestoneResponse;
import tn.esprit.backend.DTO.InvestmentHoldingResponse;
import tn.esprit.backend.Entities.DealPipeline;
import tn.esprit.backend.enums.DealStatus;
import tn.esprit.backend.Entities.InvestmentHolding;
import tn.esprit.backend.Entities.InvestmentHoldingMilestone;
import tn.esprit.backend.enums.InvestmentHoldingStatus;
import tn.esprit.backend.enums.InvestmentMilestoneStatus;
import tn.esprit.backend.Entities.InvestmentRequest;
import tn.esprit.backend.Repositories.DealPipelineRepo;
import tn.esprit.backend.Repositories.InvestmentHoldingMilestoneRepo;
import tn.esprit.backend.Repositories.InvestmentHoldingRepo;
import tn.esprit.backend.config.StripeProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentHoldingServiceImpl implements InvestmentHoldingService {
    private static final List<DateTimeFormatter> HUMAN_DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    );

    private final InvestmentHoldingRepo holdingRepo;
    private final InvestmentHoldingMilestoneRepo milestoneRepo;
    private final InvestmentRequestService investmentRequestService;
    private final DealPipelineRepo dealPipelineRepo;
    private final StripePaymentService stripePaymentService;
    private final StripeProperties stripeProperties;

    @Override
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(String requestId, CreateCheckoutSessionRequest request, RequestActor actor) {
        requireRole(actor, UserRole.INVESTOR, "Only the linked investor can create a checkout session.");

        InvestmentRequest investmentRequest = investmentRequestService.getInvestmentRequest(requestId);
        if (!investmentRequest.getInvestorId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("Only the investor linked to this request can create a checkout session.");
        }

        DealPipeline deal = resolveDealForRequest(requestId);
        if (deal.getStatus() != DealStatus.DUE_DILIGENCE) {
            throw new IllegalArgumentException("A checkout session can only be created when the deal is in DUE_DILIGENCE.");
        }

        validateCheckoutRequest(request);

        Long amountTnd = request.getAmountTnd();
        BigDecimal amountEur = convertTndToEur(amountTnd);
        long stripeAmount = convertEurToStripeMinorUnits(amountEur);

        StripeCheckoutSessionData checkoutSession = stripePaymentService.createCheckoutSession(
                stripeAmount,
                requestId,
                investmentRequest.getInvestorId(),
                investmentRequest.getStartupId()
        );

        InvestmentHolding existing = holdingRepo.findByInvestmentRequestId(requestId).orElse(null);
        if (existing != null
                && existing.getStatus() != InvestmentHoldingStatus.CANCELLED
                && existing.getStatus() != InvestmentHoldingStatus.REFUNDED
                && existing.getStatus() != InvestmentHoldingStatus.WAITING_PAYMENT) {
            throw new IllegalArgumentException("A holding already exists for this investment request.");
        }

        InvestmentHolding holding = existing == null ? new InvestmentHolding() : existing;
        if (holding.getCreatedAt() == null) {
            holding.setCreatedAt(LocalDateTime.now());
        }

        holding.setInvestmentRequestId(requestId);
        holding.setInvestorId(investmentRequest.getInvestorId());
        holding.setStartupId(investmentRequest.getStartupId());
        holding.setAmountTnd(amountTnd);
        holding.setAmountEur(amountEur);
        holding.setCurrencyDisplayed("TND");
        holding.setStripeCurrency("eur");
        holding.setStatus(InvestmentHoldingStatus.WAITING_PAYMENT);
        holding.setStripeCheckoutSessionId(checkoutSession.getSessionId());
        holding.setStripePaymentIntentId(null);
        holding.setFundedAt(null);

        InvestmentHolding savedHolding = holdingRepo.save(holding);
        return CheckoutSessionResponse.builder()
                .holdingId(savedHolding.getId())
                .amountTnd(savedHolding.getAmountTnd())
                .amountEur(savedHolding.getAmountEur())
                .checkoutUrl(checkoutSession.getCheckoutUrl())
                .build();
    }

    @Override
    @Transactional
    public InvestmentHoldingResponse confirmCheckout(ConfirmCheckoutRequest request) {
        if (request == null || request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new IllegalArgumentException("Checkout session id is required.");
        }

        StripeCheckoutSessionDetails session = stripePaymentService.getCheckoutSessionDetails(request.getSessionId().trim());
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new IllegalArgumentException("Stripe Checkout payment is not completed yet. Current status: " + session.getPaymentStatus());
        }

        InvestmentHolding holding = holdingRepo.findByStripeCheckoutSessionId(session.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("No holding matches this Stripe Checkout session."));

        if (holding.getStatus() == InvestmentHoldingStatus.CANCELLED || holding.getStatus() == InvestmentHoldingStatus.REFUNDED) {
            throw new IllegalArgumentException("This holding can no longer be confirmed.");
        }

        if (holding.getFundedAt() == null) {
            holding.setFundedAt(LocalDateTime.now());
        }
        holding.setStatus(InvestmentHoldingStatus.FUNDS_HELD);
        if (session.getPaymentIntentId() != null && !session.getPaymentIntentId().isBlank()) {
            holding.setStripePaymentIntentId(session.getPaymentIntentId());
        }

        return toResponse(holdingRepo.save(holding));
    }

    @Override
    public InvestmentHoldingResponse getHolding(String holdingId, RequestActor actor) {
        InvestmentHolding holding = requireHolding(holdingId);
        authorizeHoldingAccess(holding, actor, true);
        return toResponse(holding);
    }

    @Override
    public InvestmentHoldingResponse getHoldingByRequestId(String requestId, RequestActor actor) {
        InvestmentHolding holding = holdingRepo.findByInvestmentRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("No holding found for this investment request."));
        authorizeHoldingAccess(holding, actor, true);
        return toResponse(holding);
    }

    @Override
    @Transactional
    public InvestmentHoldingResponse requestRelease(String holdingId, RequestActor actor) {
        InvestmentHolding holding = requireHolding(holdingId);
        requireRole(actor, UserRole.STARTUP, "Only the startup can request fund release.");
        ensureStartupAccess(holding, actor);
        ensureStatus(holding, List.of(InvestmentHoldingStatus.FUNDS_HELD), "Release can only be requested when funds are held.");

        holding.setStatus(InvestmentHoldingStatus.RELEASE_REQUESTED);
        return toResponse(holdingRepo.save(holding));
    }

    @Override
    @Transactional
    public InvestmentHoldingResponse releaseFunds(String holdingId, RequestActor actor) {
        InvestmentHolding holding = requireHolding(holdingId);
        requireRole(actor, UserRole.ADMIN, "Only an admin can release funds.");
        ensureNotDisputed(holding);
        ensureStatus(
                holding,
                List.of(InvestmentHoldingStatus.FUNDS_HELD, InvestmentHoldingStatus.RELEASE_REQUESTED),
                "Funds can only be released from FUNDS_HELD or RELEASE_REQUESTED."
        );

        List<InvestmentHoldingMilestone> milestones = milestoneRepo.findByHoldingIdOrderByDueDateAsc(holdingId);
        if (!milestones.isEmpty()) {
            throw new IllegalArgumentException("This holding uses milestone payments. Release milestones individually.");
        }

        holding.setStatus(InvestmentHoldingStatus.RELEASED);
        holding.setReleasedAt(LocalDateTime.now());
        return toResponse(holdingRepo.save(holding));
    }

    @Override
    @Transactional
    public InvestmentHoldingResponse dispute(String holdingId, RequestActor actor) {
        InvestmentHolding holding = requireHolding(holdingId);
        requireRole(actor, UserRole.INVESTOR, "Only the investor can open a dispute.");
        ensureInvestorAccess(holding, actor);

        if (holding.getStatus() == InvestmentHoldingStatus.RELEASED
                || holding.getStatus() == InvestmentHoldingStatus.REFUNDED
                || holding.getStatus() == InvestmentHoldingStatus.CANCELLED) {
            throw new IllegalArgumentException("This holding can no longer be disputed.");
        }

        holding.setStatus(InvestmentHoldingStatus.DISPUTED);
        return toResponse(holdingRepo.save(holding));
    }

    @Override
    @Transactional
    public InvestmentHoldingResponse refund(String holdingId, RequestActor actor) {
        InvestmentHolding holding = requireHolding(holdingId);
        requireRole(actor, UserRole.ADMIN, "Only an admin can mark a holding as refunded.");
        ensureNotReleased(holding);

        holding.setStatus(InvestmentHoldingStatus.REFUNDED);
        holding.setCancelledAt(LocalDateTime.now());
        return toResponse(holdingRepo.save(holding));
    }

    @Override
    @Transactional
    public InvestmentHoldingMilestoneResponse createMilestone(String holdingId, CreateMilestoneRequest request, RequestActor actor) {
        InvestmentHolding holding = requireHolding(holdingId);
        if (actor.getRole() != UserRole.INVESTOR && actor.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only the investor or an admin can create milestones.");
        }
        if (actor.getRole() == UserRole.INVESTOR) {
            ensureInvestorAccess(holding, actor);
        }
        if (holding.getStatus() == InvestmentHoldingStatus.DISPUTED
                || holding.getStatus() == InvestmentHoldingStatus.RELEASED
                || holding.getStatus() == InvestmentHoldingStatus.REFUNDED) {
            throw new IllegalArgumentException("Milestones cannot be added for this holding status.");
        }
        validateMilestoneRequest(request);

        long usedAmount = milestoneRepo.findByHoldingIdOrderByDueDateAsc(holdingId).stream()
                .mapToLong(m -> m.getAmount() == null ? 0L : m.getAmount())
                .sum();
        if (usedAmount + request.getAmount() > holding.getAmountTnd()) {
            throw new IllegalArgumentException("Milestone total exceeds the holding amount.");
        }

        InvestmentHoldingMilestone milestone = InvestmentHoldingMilestone.builder()
                .holdingId(holdingId)
                .title(request.getTitle().trim())
                .amount(request.getAmount())
                .dueDate(parseDate(request.getDueDate(), "dueDate"))
                .status(InvestmentMilestoneStatus.PENDING)
                .validatedByInvestor(false)
                .validatedByStartup(false)
                .build();

        return toMilestoneResponse(milestoneRepo.save(milestone));
    }

    @Override
    @Transactional
    public InvestmentHoldingMilestoneResponse validateMilestone(String milestoneId, RequestActor actor) {
        InvestmentHoldingMilestone milestone = requireMilestone(milestoneId);
        InvestmentHolding holding = requireHolding(milestone.getHoldingId());
        authorizeHoldingAccess(holding, actor, false);

        if (actor.getRole() != UserRole.INVESTOR && actor.getRole() != UserRole.STARTUP) {
            throw new IllegalArgumentException("Only the investor or the startup can validate a milestone.");
        }
        if (milestone.getStatus() == InvestmentMilestoneStatus.RELEASED || milestone.getStatus() == InvestmentMilestoneStatus.BLOCKED) {
            throw new IllegalArgumentException("This milestone cannot be validated anymore.");
        }

        if (actor.getRole() == UserRole.INVESTOR) {
            ensureInvestorAccess(holding, actor);
            milestone.setValidatedByInvestor(true);
        } else {
            ensureStartupAccess(holding, actor);
            milestone.setValidatedByStartup(true);
        }

        if (milestone.isValidatedByInvestor() && milestone.isValidatedByStartup()) {
            milestone.setStatus(InvestmentMilestoneStatus.VALIDATED);
        }

        return toMilestoneResponse(milestoneRepo.save(milestone));
    }

    @Override
    @Transactional
    public InvestmentHoldingMilestoneResponse releaseMilestone(String milestoneId, RequestActor actor) {
        requireRole(actor, UserRole.ADMIN, "Only an admin can release a milestone.");

        InvestmentHoldingMilestone milestone = requireMilestone(milestoneId);
        InvestmentHolding holding = requireHolding(milestone.getHoldingId());
        ensureNotDisputed(holding);
        ensureStatus(
                holding,
                List.of(InvestmentHoldingStatus.FUNDS_HELD, InvestmentHoldingStatus.RELEASE_REQUESTED),
                "Milestones can only be released when funds are held or release is requested."
        );

        if (milestone.getStatus() != InvestmentMilestoneStatus.VALIDATED) {
            throw new IllegalArgumentException("Only validated milestones can be released.");
        }

        milestone.setStatus(InvestmentMilestoneStatus.RELEASED);
        milestone.setReleasedAt(LocalDateTime.now());
        InvestmentHoldingMilestone saved = milestoneRepo.save(milestone);

        boolean allReleased = milestoneRepo.findByHoldingIdOrderByDueDateAsc(holding.getId()).stream()
                .allMatch(item -> item.getStatus() == InvestmentMilestoneStatus.RELEASED);
        if (allReleased) {
            holding.setStatus(InvestmentHoldingStatus.RELEASED);
            holding.setReleasedAt(LocalDateTime.now());
            holdingRepo.save(holding);
        }

        return toMilestoneResponse(saved);
    }

    private void validateCheckoutRequest(CreateCheckoutSessionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Checkout payload is required.");
        }
        if (request.getAmountTnd() == null || request.getAmountTnd() <= 0) {
            throw new IllegalArgumentException("amountTnd must be greater than zero.");
        }
    }

    private void validateMilestoneRequest(CreateMilestoneRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Milestone payload is required.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Milestone title is required.");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Milestone amount must be greater than zero.");
        }
        parseDate(request.getDueDate(), "dueDate");
    }

    private DealPipeline resolveDealForRequest(String requestId) {
        return dealPipelineRepo.findAllByRequestId(requestId).stream()
                .max(Comparator.comparing(
                        DealPipeline::getStatusChangedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).thenComparing(DealPipeline::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new IllegalArgumentException("No deal pipeline exists for this investment request."));
    }

    private BigDecimal convertTndToEur(Long amountTnd) {
        return BigDecimal.valueOf(amountTnd)
                .divide(stripeProperties.getExchangeRateTndEur(), 2, RoundingMode.HALF_UP);
    }

    private long convertEurToStripeMinorUnits(BigDecimal amountEur) {
        return amountEur.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private LocalDateTime parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        String normalized = value.trim();

        for (DateTimeFormatter formatter : HUMAN_DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atTime(LocalTime.of(9, 0));
        } catch (DateTimeParseException ignored) {
        }

        throw new IllegalArgumentException(
                fieldName + " must look like 2026-05-30 14:30 or 2026-05-30T14:30."
        );
    }

    private InvestmentHolding requireHolding(String holdingId) {
        return holdingRepo.findById(holdingId)
                .orElseThrow(() -> new IllegalArgumentException("Investment holding not found."));
    }

    private InvestmentHoldingMilestone requireMilestone(String milestoneId) {
        return milestoneRepo.findById(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Investment milestone not found."));
    }

    private void authorizeHoldingAccess(InvestmentHolding holding, RequestActor actor, boolean allowAdmin) {
        if (actor.getRole() == UserRole.INVESTOR) {
            ensureInvestorAccess(holding, actor);
            return;
        }
        if (actor.getRole() == UserRole.STARTUP) {
            ensureStartupAccess(holding, actor);
            return;
        }
        if (allowAdmin && actor.getRole() == UserRole.ADMIN) {
            return;
        }
        throw new IllegalArgumentException("You are not allowed to access this holding.");
    }

    private void ensureInvestorAccess(InvestmentHolding holding, RequestActor actor) {
        if (!holding.getInvestorId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("This holding does not belong to the current investor.");
        }
    }

    private void ensureStartupAccess(InvestmentHolding holding, RequestActor actor) {
        if (!holding.getStartupId().equals(actor.getUserId())) {
            throw new IllegalArgumentException("This holding does not belong to the current startup.");
        }
    }

    private void requireRole(RequestActor actor, UserRole expectedRole, String message) {
        if (actor.getRole() != expectedRole) {
            throw new IllegalArgumentException(message);
        }
    }

    private void ensureStatus(InvestmentHolding holding, List<InvestmentHoldingStatus> expectedStatuses, String message) {
        if (!expectedStatuses.contains(holding.getStatus())) {
            throw new IllegalArgumentException(message);
        }
    }

    private void ensureNotDisputed(InvestmentHolding holding) {
        if (holding.getStatus() == InvestmentHoldingStatus.DISPUTED) {
            throw new IllegalArgumentException("Funds cannot be released while the holding is disputed.");
        }
    }

    private void ensureNotReleased(InvestmentHolding holding) {
        if (holding.getStatus() == InvestmentHoldingStatus.RELEASED) {
            throw new IllegalArgumentException("Released holdings cannot be refunded.");
        }
        if (holding.getStatus() == InvestmentHoldingStatus.REFUNDED) {
            throw new IllegalArgumentException("This holding has already been refunded.");
        }
    }

    private InvestmentHoldingResponse toResponse(InvestmentHolding holding) {
        List<InvestmentHoldingMilestoneResponse> milestones = milestoneRepo.findByHoldingIdOrderByDueDateAsc(holding.getId()).stream()
                .map(this::toMilestoneResponse)
                .toList();

        return InvestmentHoldingResponse.builder()
                .id(holding.getId())
                .investmentRequestId(holding.getInvestmentRequestId())
                .investorId(holding.getInvestorId())
                .startupId(holding.getStartupId())
                .amountTnd(holding.getAmountTnd())
                .amountEur(holding.getAmountEur())
                .currencyDisplayed(holding.getCurrencyDisplayed())
                .stripeCurrency(holding.getStripeCurrency())
                .status(holding.getStatus())
                .stripeCheckoutSessionId(holding.getStripeCheckoutSessionId())
                .stripePaymentIntentId(holding.getStripePaymentIntentId())
                .createdAt(holding.getCreatedAt())
                .fundedAt(holding.getFundedAt())
                .releasedAt(holding.getReleasedAt())
                .cancelledAt(holding.getCancelledAt())
                .milestones(milestones)
                .build();
    }

    private InvestmentHoldingMilestoneResponse toMilestoneResponse(InvestmentHoldingMilestone milestone) {
        return InvestmentHoldingMilestoneResponse.builder()
                .id(milestone.getId())
                .holdingId(milestone.getHoldingId())
                .title(milestone.getTitle())
                .amount(milestone.getAmount())
                .dueDate(milestone.getDueDate())
                .status(milestone.getStatus())
                .validatedByInvestor(milestone.isValidatedByInvestor())
                .validatedByStartup(milestone.isValidatedByStartup())
                .releasedAt(milestone.getReleasedAt())
                .build();
    }
}
