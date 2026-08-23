package com.eyram.dev.church_project_spring.service;

import com.eyram.dev.church_project_spring.DTO.response.TrackingByPhoneChallengeResponse;
import com.eyram.dev.church_project_spring.entities.Demande;
import com.eyram.dev.church_project_spring.repositories.DemandeDateRepository;
import com.eyram.dev.church_project_spring.repositories.DemandeRepository;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrackingPhoneOtpServiceTest {

    private final DemandeRepository demandeRepository = mock(DemandeRepository.class);
    private final DemandeDateRepository demandeDateRepository = mock(DemandeDateRepository.class);
    private final AppMailService mailService = mock(AppMailService.class);
    private final TrackingPhoneOtpService service =
            new TrackingPhoneOtpService(demandeRepository, demandeDateRepository, mailService);

    @Test
    void requestOtpDoesNotExposeWhetherPhoneExistsWhenNoEmailIsAvailable() {
        String phone = "+22890123456";
        Demande demande = mock(Demande.class);
        when(demande.getId()).thenReturn(1L);
        when(demande.getCodeSuivie()).thenReturn("MS-TEST-ABC234");
        when(demande.getEmailFidele()).thenReturn(null);
        when(demandeDateRepository.findByDemande_IdInAndStatusDelFalseOrderByOrdreAsc(List.of(1L)))
                .thenReturn(List.of());

        when(demandeRepository.findAllByTelFideleInChronological(
                TrackingPhoneOtpService.phoneLookupVariants(phone)
        )).thenReturn(List.of(demande));
        TrackingByPhoneChallengeResponse knownWithoutEmail = service.requestOtp(phone);

        when(demandeRepository.findAllByTelFideleInChronological(
                TrackingPhoneOtpService.phoneLookupVariants(phone)
        )).thenReturn(List.of());
        TrackingByPhoneChallengeResponse unknown = service.requestOtp(phone);

        assertEquals(unknown, knownWithoutEmail);
        assertTrue(knownWithoutEmail.codes().isEmpty());
        assertTrue(knownWithoutEmail.demandes().isEmpty());
        verifyNoInteractions(mailService);
    }

    @Test
    void requestOtpNeverReturnsTrackingDataBeforeVerification() {
        String phone = "+22890123456";
        Demande demande = mock(Demande.class);
        when(demande.getId()).thenReturn(null);
        when(demande.getCodeSuivie()).thenReturn("MS-TEST-ABC234");
        when(demande.getEmailFidele()).thenReturn("fidele@example.com");
        when(demandeDateRepository.findByDemande_IdInAndStatusDelFalseOrderByOrdreAsc(List.of(1L)))
                .thenReturn(List.of());
        when(demandeRepository.findAllByTelFideleInChronological(
                TrackingPhoneOtpService.phoneLookupVariants(phone)
        )).thenReturn(List.of(demande));

        TrackingByPhoneChallengeResponse response = service.requestOtp(phone);

        assertTrue(response.codes().isEmpty());
        assertTrue(response.demandes().isEmpty());
        assertEquals(null, response.emailMasked());
        org.mockito.Mockito.verify(mailService).sendText(
                org.mockito.ArgumentMatchers.eq("fidele@example.com"),
                anyString(),
                anyString()
        );
    }
}
