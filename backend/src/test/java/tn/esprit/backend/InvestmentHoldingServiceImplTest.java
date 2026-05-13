package tn.esprit.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend.DTO.ConfirmCheckoutRequest;
import tn.esprit.backend.DTO.CreateCheckoutSessionRequest;
import tn.esprit.backend.DTO.CreateMilestoneRequest;
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
import tn.esprit.backend.Services.InvestmentHoldingServiceImpl;
import tn.esprit.backend.Services.RequestActor;
import tn.esprit.backend.Services.StripeCheckoutSessionData;
import tn.esprit.backend.Services.StripeCheckoutSessionDetails;
import tn.esprit.backend.Services.StripePaymentService;
import tn.esprit.backend.Services.UserRole;
import tn.esprit.backend.config.StripeProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentHoldingServiceImplTest {

    @Mock
    private InvestmentHoldingRepo holdingRepo;
    @Mock
    private InvestmentHoldingMilestoneRepo milestoneRepo;
    @Mock
    private tn.esprit.backend.Services.InvestmentRequestService investmentRequestService;
    @Mock
    private DealPipelineRepo dealPipelineRepo;
    @Mock
    private StripePaymentService stripePaymentService;
    @Mock
    private StripeProperties stripeProperties;

    @InjectMocks
    private InvestmentHoldingServiceImpl service;

    private InvestmentRequest request;
    private DealPipeline deal;

    @BeforeEach
    void setUp() {
        request = new InvestmentRequest();
        request.setId("req-1");
        request.setInvestorId("investor-1");
        request.setStartupId("startup-1");
        request.setTicketProposed(10000L);

        deal = new DealPipeline();
        deal.setId("deal-1");
        deal.setRequestId("req-1");
        deal.setInvestorId("investor-1");
        deal.setStartupId("startup-1");
        deal.setStatus(DealStatus.DUE_DILIGENCE);
        deal.setStatusChangedAt(LocalDateTime.now());

        lenient().when(stripeProperties.getExchangeRateTndEur()).thenReturn(new BigDecimal("3.35"));
    }

    @Test
    void shouldCreateCheckoutSessionWhenDealIsInDueDiligence() {
        CreateCheckoutSessionRequest payload = new CreateCheckoutSessionRequest();
        payload.setAmountTnd(10000L);

        when(investmentRequestService.getInvestmentRequest("req-1")).thenReturn(request);
        when(dealPipelineRepo.findAllByRequestId("req-1")).thenReturn(List.of(deal));
        when(holdingRepo.findByInvestmentRequestId("req-1")).thenReturn(Optional.empty());
        when(stripePaymentService.createCheckoutSession(anyLong(), any(), any(), any()))
                .thenReturn(new StripeCheckoutSessionData("cs_test_1", "https://checkout.stripe.com/test"));
        when(holdingRepo.save(any(InvestmentHolding.class))).thenAnswer(invocation -> {
            InvestmentHolding holding = invocation.getArgument(0);
            holding.setId("holding-1");
            return holding;
        });

        var response = service.createCheckoutSession("req-1", payload, new RequestActor("investor-1", UserRole.INVESTOR));

        assertEquals("holding-1", response.getHoldingId());
        assertEquals(10000L, response.getAmountTnd());
        assertEquals(new BigDecimal("2985.07"), response.getAmountEur());
        assertEquals("https://checkout.stripe.com/test", response.getCheckoutUrl());
    }

    @Test
    void shouldRefuseCheckoutSessionOutsideDueDiligence() {
        CreateCheckoutSessionRequest payload = new CreateCheckoutSessionRequest();
        payload.setAmountTnd(10000L);
        deal.setStatus(DealStatus.CONTACTED);

        when(investmentRequestService.getInvestmentRequest("req-1")).thenReturn(request);
        when(dealPipelineRepo.findAllByRequestId("req-1")).thenReturn(List.of(deal));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createCheckoutSession("req-1", payload, new RequestActor("investor-1", UserRole.INVESTOR))
        );

        assertTrue(ex.getMessage().contains("DUE_DILIGENCE"));
    }

    @Test
    void shouldConfirmCheckoutWhenStripeMarksSessionPaid() {
        ConfirmCheckoutRequest payload = new ConfirmCheckoutRequest();
        payload.setSessionId("cs_test_1");

        InvestmentHolding holding = InvestmentHolding.builder()
                .id("holding-1")
                .investmentRequestId("req-1")
                .investorId("investor-1")
                .startupId("startup-1")
                .amountTnd(10000L)
                .amountEur(new BigDecimal("2985.07"))
                .currencyDisplayed("TND")
                .stripeCurrency("eur")
                .status(InvestmentHoldingStatus.WAITING_PAYMENT)
                .stripeCheckoutSessionId("cs_test_1")
                .createdAt(LocalDateTime.now())
                .build();

        when(stripePaymentService.getCheckoutSessionDetails("cs_test_1"))
                .thenReturn(new StripeCheckoutSessionDetails("cs_test_1", "paid", "pi_test_1"));
        when(holdingRepo.findByStripeCheckoutSessionId("cs_test_1")).thenReturn(Optional.of(holding));
        when(holdingRepo.save(any(InvestmentHolding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(milestoneRepo.findByHoldingIdOrderByDueDateAsc("holding-1")).thenReturn(List.of());

        var response = service.confirmCheckout(payload);

        assertEquals(InvestmentHoldingStatus.FUNDS_HELD, response.getStatus());
        assertEquals("pi_test_1", response.getStripePaymentIntentId());
        assertNotNull(response.getFundedAt());
    }

    @Test
    void shouldRefuseReleaseWhenHoldingIsDisputed() {
        InvestmentHolding holding = InvestmentHolding.builder()
                .id("holding-1")
                .investmentRequestId("req-1")
                .investorId("investor-1")
                .startupId("startup-1")
                .amountTnd(10000L)
                .amountEur(new BigDecimal("2985.07"))
                .currencyDisplayed("TND")
                .stripeCurrency("eur")
                .status(InvestmentHoldingStatus.DISPUTED)
                .build();

        when(holdingRepo.findById("holding-1")).thenReturn(Optional.of(holding));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.releaseFunds("holding-1", new RequestActor("admin", UserRole.ADMIN))
        );

        assertTrue(ex.getMessage().contains("disputed"));
    }

    @Test
    void shouldValidateMilestoneAfterInvestorAndStartupApprove() {
        InvestmentHolding holding = InvestmentHolding.builder()
                .id("holding-1")
                .investmentRequestId("req-1")
                .investorId("investor-1")
                .startupId("startup-1")
                .amountTnd(10000L)
                .amountEur(new BigDecimal("2985.07"))
                .currencyDisplayed("TND")
                .stripeCurrency("eur")
                .status(InvestmentHoldingStatus.FUNDS_HELD)
                .build();

        InvestmentHoldingMilestone milestone = InvestmentHoldingMilestone.builder()
                .id("ms-1")
                .holdingId("holding-1")
                .title("Phase 1")
                .amount(3000L)
                .dueDate(LocalDateTime.now())
                .status(InvestmentMilestoneStatus.PENDING)
                .validatedByInvestor(false)
                .validatedByStartup(false)
                .build();

        when(milestoneRepo.findById("ms-1")).thenReturn(Optional.of(milestone));
        when(holdingRepo.findById("holding-1")).thenReturn(Optional.of(holding));
        when(milestoneRepo.save(any(InvestmentHoldingMilestone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.validateMilestone("ms-1", new RequestActor("investor-1", UserRole.INVESTOR));
        var response = service.validateMilestone("ms-1", new RequestActor("startup-1", UserRole.STARTUP));

        assertEquals(InvestmentMilestoneStatus.VALIDATED, response.getStatus());
        assertTrue(response.isValidatedByInvestor());
        assertTrue(response.isValidatedByStartup());
    }

    @Test
    void shouldAcceptHumanReadableMilestoneDateTime() {
        InvestmentHolding holding = InvestmentHolding.builder()
                .id("holding-1")
                .investmentRequestId("req-1")
                .investorId("investor-1")
                .startupId("startup-1")
                .amountTnd(10000L)
                .amountEur(new BigDecimal("2985.07"))
                .currencyDisplayed("TND")
                .stripeCurrency("eur")
                .status(InvestmentHoldingStatus.FUNDS_HELD)
                .build();

        CreateMilestoneRequest payload = new CreateMilestoneRequest();
        payload.setTitle("Kickoff");
        payload.setAmount(2500L);
        payload.setDueDate("2026-05-30 14:30");

        when(holdingRepo.findById("holding-1")).thenReturn(Optional.of(holding));
        when(milestoneRepo.findByHoldingIdOrderByDueDateAsc("holding-1")).thenReturn(List.of());
        when(milestoneRepo.save(any(InvestmentHoldingMilestone.class))).thenAnswer(invocation -> {
            InvestmentHoldingMilestone milestone = invocation.getArgument(0);
            milestone.setId("ms-2");
            return milestone;
        });

        var response = service.createMilestone("holding-1", payload, new RequestActor("investor-1", UserRole.INVESTOR));

        assertEquals("ms-2", response.getId());
        assertEquals(InvestmentMilestoneStatus.PENDING, response.getStatus());
        assertEquals(LocalDateTime.of(2026, 5, 30, 14, 30), response.getDueDate());
    }
}
