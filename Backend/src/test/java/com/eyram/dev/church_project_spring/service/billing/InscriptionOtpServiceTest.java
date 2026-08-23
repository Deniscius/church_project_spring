package com.eyram.dev.church_project_spring.service.billing;

import com.eyram.dev.church_project_spring.DTO.response.InscriptionOtpVerifyResponse;
import com.eyram.dev.church_project_spring.repositories.ParoisseInscriptionRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.ProfessionalEmailService;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscriptionOtpServiceTest {

    @Mock
    private AppMailService mailService;
    @Mock
    private ProfessionalEmailService professionalEmailService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParoisseInscriptionRepository inscriptionRepository;

    private InscriptionOtpService service;

    @BeforeEach
    void setUp() {
        service = new InscriptionOtpService(
                mailService,
                professionalEmailService,
                userRepository,
                inscriptionRepository
        );
    }

    @Test
    void verifyOtp_doesNotExposePasswordInResponse_andEmailsIt() throws Exception {
        when(professionalEmailService.slugify("Saint Joseph")).thenReturn("saint-joseph");
        when(professionalEmailService.slugify("Jean")).thenReturn("jean");
        when(professionalEmailService.slugify("Dupont")).thenReturn("dupont");
        when(userRepository.existsByUsernameIgnoreCaseAndStatusDelFalse(anyString())).thenReturn(false);
        when(inscriptionRepository.existsByAdminUsernameIgnoreCaseAndStatusDelFalse(anyString())).thenReturn(false);

        putOtp("admin@example.com", "123456");

        InscriptionOtpVerifyResponse res = service.verifyOtp(
                "admin@example.com",
                "123456",
                "Jean",
                "Dupont",
                "Saint Joseph"
        );

        assertEquals("admin@example.com", res.email());
        assertNotNull(res.otpProof());
        assertFalse(res.otpProof().isBlank());
        assertEquals("admin.saint-joseph", res.adminUsername());
        assertTrue(res.message().toLowerCase().contains("mot de passe"));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendText(
                eq("admin@example.com"),
                eq("Vos identifiants — Missanye"),
                bodyCaptor.capture()
        );
        String mailBody = bodyCaptor.getValue();
        assertTrue(mailBody.contains("Identifiant : admin.saint-joseph"));
        assertTrue(mailBody.contains("Mot de passe temporaire :"));
        assertTrue(mailBody.contains("n'apparaît que dans cet e-mail"));

        // Preuve serveur conserve le secret pour la soumission ultérieure
        var proof = service.requireValidProof(
                "admin@example.com",
                res.otpProof(),
                res.adminUsername(),
                "Jean",
                "Dupont",
                "Saint Joseph"
        );
        assertNotNull(proof.password());
        assertFalse(proof.password().isBlank());
        assertFalse(mailBody.contains(res.otpProof()));

        assertThrows(BusinessRuleException.class, () -> service.requireValidProof(
                "admin@example.com",
                res.otpProof(),
                res.adminUsername(),
                "Jean",
                "Dupont",
                "Saint Joseph"
        ));
    }

    @Test
    void proofRejectsParishChangedAfterOtpVerification() throws Exception {
        when(professionalEmailService.slugify("Saint Joseph")).thenReturn("saint-joseph");
        when(professionalEmailService.slugify("Jean")).thenReturn("jean");
        when(professionalEmailService.slugify("Dupont")).thenReturn("dupont");
        when(userRepository.existsByUsernameIgnoreCaseAndStatusDelFalse(anyString())).thenReturn(false);
        when(inscriptionRepository.existsByAdminUsernameIgnoreCaseAndStatusDelFalse(anyString())).thenReturn(false);

        putOtp("admin@example.com", "123456");
        InscriptionOtpVerifyResponse res = service.verifyOtp(
                "admin@example.com",
                "123456",
                "Jean",
                "Dupont",
                "Saint Joseph"
        );

        assertThrows(BusinessRuleException.class, () -> service.requireValidProof(
                "admin@example.com",
                res.otpProof(),
                res.adminUsername(),
                "Jean",
                "Dupont",
                "Sainte Thérèse"
        ));
    }

    @Test
    void verifyOtp_rejectsWrongCode() throws Exception {
        putOtp("admin@example.com", "123456");
        assertThrows(BusinessRuleException.class, () ->
                service.verifyOtp("admin@example.com", "000000", "Jean", "Dupont", "Saint Joseph"));
    }

    @SuppressWarnings("unchecked")
    private void putOtp(String email, String code) throws Exception {
        Field field = InscriptionOtpService.class.getDeclaredField("otps");
        field.setAccessible(true);
        ConcurrentHashMap<String, InscriptionOtpService.OtpEntry> otps =
                (ConcurrentHashMap<String, InscriptionOtpService.OtpEntry>) field.get(service);
        otps.put(email, new InscriptionOtpService.OtpEntry(code, Instant.now().plusSeconds(600), 0));
    }
}
