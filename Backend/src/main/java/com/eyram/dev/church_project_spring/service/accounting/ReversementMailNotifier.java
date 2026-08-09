package com.eyram.dev.church_project_spring.service.accounting;

import com.eyram.dev.church_project_spring.entities.DemandeReversement;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.StatutReversement;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.mail.AppMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Notifications e-mail liées aux demandes de reversement.
 * Les échecs d'envoi sont journalisés sans bloquer le métier.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReversementMailNotifier {

    private static final NumberFormat FCFA = NumberFormat.getIntegerInstance(Locale.FRANCE);

    private final AppMailService appMailService;
    private final UserRepository userRepository;

    public void notifyComptablesNouvelleDemande(DemandeReversement demande) {
        String subject = "[Missanye] Nouvelle demande de reversement — " + safeParish(demande);
        String body = """
                Bonjour,

                Une paroisse a demandé un reversement.

                Paroisse : %s
                Montant : %s FCFA
                Banque : %s
                Titulaire : %s
                RIB / compte : %s
                Motif : %s
                Référence demande : %s

                Connectez-vous à l'espace Comptable Missanye pour traiter le virement
                (menu Reversements).

                — Missanye
                """.formatted(
                safeParish(demande),
                formatMontant(demande.getMontant()),
                blank(demande.getNomBanque()),
                blank(demande.getTitulaireCompte()),
                blank(demande.getIbanOrRib()),
                blank(demande.getMotif()),
                demande.getPublicId()
        );

        var comptables = userRepository
                .findByStatusDelFalseAndIsActiveTrueAndIsGlobalTrueAndRole(UserRole.COMPTABLE);
        if (comptables.isEmpty()) {
            log.warn("Demande de reversement {} sans COMPTABLE actif à notifier", demande.getPublicId());
            return;
        }
        for (User user : comptables) {
            sendQuietly(user.getEmail(), subject, body);
        }
    }

    public void notifyParoisseDecision(DemandeReversement demande) {
        String to = demande.getParoisse() != null ? demande.getParoisse().getEmail() : null;
        if (!StringUtils.hasText(to)) {
            log.info("Pas d'e-mail paroisse pour notifier la décision {}", demande.getPublicId());
            return;
        }

        boolean paye = demande.getStatut() == StatutReversement.PAYE;
        String subject = paye
                ? "[Missanye] Reversement effectué — " + safeParish(demande)
                : "[Missanye] Reversement rejeté — " + safeParish(demande);

        String body = paye
                ? """
                Bonjour,

                Le reversement demandé a été marqué comme payé.

                Paroisse : %s
                Montant : %s FCFA
                Référence virement : %s
                Traité par : %s

                Vérifiez la réception sur le compte bancaire déclaré.

                — Missanye
                """.formatted(
                safeParish(demande),
                formatMontant(demande.getMontant()),
                blank(demande.getReferenceVirement()),
                blank(demande.getTraitePar())
                )
                : """
                Bonjour,

                La demande de reversement a été rejetée.

                Paroisse : %s
                Montant : %s FCFA
                Motif : %s
                Traité par : %s

                Le montant a été remis à disposition sur le solde de la paroisse.
                Vous pouvez corriger les informations puis renouveler la demande.

                — Missanye
                """.formatted(
                safeParish(demande),
                formatMontant(demande.getMontant()),
                blank(demande.getMotif()),
                blank(demande.getTraitePar())
                );

        sendQuietly(to, subject, body);
    }

    private void sendQuietly(String to, String subject, String body) {
        if (!StringUtils.hasText(to)) {
            return;
        }
        try {
            appMailService.sendTextAsync(to.trim(), subject, body);
        } catch (Exception ex) {
            log.warn("Notification reversement non envoyée à {}: {}", to, ex.getMessage());
        }
    }

    private static String safeParish(DemandeReversement demande) {
        if (demande.getParoisse() == null || !StringUtils.hasText(demande.getParoisse().getNom())) {
            return "Paroisse";
        }
        return demande.getParoisse().getNom();
    }

    private static String blank(String value) {
        return StringUtils.hasText(value) ? value.trim() : "—";
    }

    private static String formatMontant(Integer montant) {
        if (montant == null) {
            return "0";
        }
        return FCFA.format(montant);
    }
}
