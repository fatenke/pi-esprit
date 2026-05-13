package tn.esprit.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend.DTO.SignNdaRequest;
import tn.esprit.backend.Entities.DataRoom;
import tn.esprit.backend.Entities.NdaAgreement;
import tn.esprit.backend.enums.NdaStatus;
import tn.esprit.backend.enums.SignatureType;
import tn.esprit.backend.Repositories.DataRoomRepo;
import tn.esprit.backend.Repositories.NdaAgreementRepository;
import tn.esprit.backend.Repositories.NdaSignatureRepository;
import tn.esprit.backend.Services.NdaServiceImpl;
import tn.esprit.backend.Services.RequestActor;
import tn.esprit.backend.Services.UserRole;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NdaServiceImplTest {

    @Mock
    private DataRoomRepo dataRoomRepo;
    @Mock
    private NdaAgreementRepository ndaAgreementRepository;
    @Mock
    private NdaSignatureRepository ndaSignatureRepository;

    @InjectMocks
    private NdaServiceImpl ndaService;

    private DataRoom room;
    private NdaAgreement pendingAgreement;

    @BeforeEach
    void setUp() {
        room = DataRoom.builder()
                .id("room-1")
                .startupId("startup-1")
                .investorId("investor-1")
                .dealId("deal-1")
                .ndaSigned(false)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        pendingAgreement = NdaAgreement.builder()
                .id("nda-1")
                .dataRoomId("room-1")
                .startupId("startup-1")
                .investorId("investor-1")
                .ndaContent("NDA content")
                .ndaHash("hash-nda")
                .status(NdaStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldCreateNdaWhenMissing() {
        when(dataRoomRepo.findById("room-1")).thenReturn(Optional.of(room));
        when(ndaAgreementRepository.findByDataRoomId("room-1")).thenReturn(Optional.empty());
        when(ndaAgreementRepository.save(any(NdaAgreement.class))).thenAnswer(invocation -> {
            NdaAgreement agreement = invocation.getArgument(0);
            agreement.setId("nda-new");
            return agreement;
        });

        NdaAgreement agreement = ndaService.createOrGetNda("room-1");

        assertEquals("nda-new", agreement.getId());
        assertEquals(NdaStatus.PENDING, agreement.getStatus());
        assertNotNull(agreement.getNdaHash());
        assertTrue(agreement.getNdaHash().length() >= 32);
    }

    @Test
    void shouldSignDrawnNdaForLinkedInvestor() {
        SignNdaRequest request = new SignNdaRequest();
        request.setAcceptedTerms(true);
        request.setSignerFullName("Amin Ben Ali");
        request.setSignatureType(SignatureType.DRAWN);
        request.setSignatureImageBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wn7lN4AAAAASUVORK5CYII=");

        when(dataRoomRepo.findById("room-1")).thenReturn(Optional.of(room));
        when(ndaAgreementRepository.findByDataRoomId("room-1")).thenReturn(Optional.of(pendingAgreement));
        when(ndaSignatureRepository.save(any())).thenAnswer(invocation -> {
            var signature = invocation.getArgument(0, tn.esprit.backend.Entities.NdaSignature.class);
            if (signature.getId() == null) {
                signature.setId("sig-1");
            }
            return signature;
        });
        when(ndaAgreementRepository.save(any(NdaAgreement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dataRoomRepo.save(any(DataRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = ndaService.signNda(
                "room-1",
                request,
                new RequestActor("investor-1", UserRole.INVESTOR),
                "127.0.0.1",
                "JUnit"
        );

        assertEquals(NdaStatus.SIGNED, response.getStatus());
        assertEquals("nda-1", response.getNdaId());
        assertNotNull(response.getSignedAt());
        assertNotNull(response.getSignatureHash());
    }

    @Test
    void shouldRejectSignatureWithoutAcceptedTerms() {
        SignNdaRequest request = new SignNdaRequest();
        request.setAcceptedTerms(false);
        request.setSignerFullName("Amin Ben Ali");
        request.setSignatureType(SignatureType.DRAWN);
        request.setSignatureImageBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wn7lN4AAAAASUVORK5CYII=");

        when(dataRoomRepo.findById("room-1")).thenReturn(Optional.of(room));
        when(ndaAgreementRepository.findByDataRoomId("room-1")).thenReturn(Optional.of(pendingAgreement));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ndaService.signNda(
                        "room-1",
                        request,
                        new RequestActor("investor-1", UserRole.INVESTOR),
                        "127.0.0.1",
                        "JUnit"
                )
        );

        assertTrue(ex.getMessage().contains("accept"));
    }

    @Test
    void shouldRejectTypedSignatureForNda() {
        SignNdaRequest request = new SignNdaRequest();
        request.setAcceptedTerms(true);
        request.setSignerFullName("Amin Ben Ali");
        request.setSignatureType(SignatureType.TYPED);
        request.setTypedSignature("Amin Ben Ali");

        when(dataRoomRepo.findById("room-1")).thenReturn(Optional.of(room));
        when(ndaAgreementRepository.findByDataRoomId("room-1")).thenReturn(Optional.of(pendingAgreement));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ndaService.signNda(
                        "room-1",
                        request,
                        new RequestActor("investor-1", UserRole.INVESTOR),
                        "127.0.0.1",
                        "JUnit"
                )
        );

        assertTrue(ex.getMessage().contains("drawn"));
    }
}
