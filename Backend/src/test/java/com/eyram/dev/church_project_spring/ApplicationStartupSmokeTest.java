package com.eyram.dev.church_project_spring;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.eyram.dev.church_project_spring.entities.ForfaitTarif;
import com.eyram.dev.church_project_spring.entities.Horaire;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.TypeDemande;
import com.eyram.dev.church_project_spring.entities.TypePaiement;
import com.eyram.dev.church_project_spring.entities.User;
import com.eyram.dev.church_project_spring.enums.JourSemaine;
import com.eyram.dev.church_project_spring.enums.ModePaiement;
import com.eyram.dev.church_project_spring.enums.NatureForfaitEnum;
import com.eyram.dev.church_project_spring.enums.StatutTenant;
import com.eyram.dev.church_project_spring.enums.UserRole;
import com.eyram.dev.church_project_spring.repositories.ForfaitTarifRepository;
import com.eyram.dev.church_project_spring.repositories.HoraireRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.TypeDemandeRepository;
import com.eyram.dev.church_project_spring.repositories.TypePaiementRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.service.tenant.TenantCatalogBootstrapService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin-seed.enabled=false",
                "app.tenant-catalog.backfill-on-startup=false",
                "app.demande.unpaid-auto-cancel-enabled=false",
                "app.demande.unpaid-reminder-enabled=false",
                "app.demande.celebration-reminder-enabled=false",
                "app.demande.celebration-auto-complete-enabled=false",
                "fedapay.enabled=false",
                "spring.flyway.repair-on-migrate=false"
        }
)
@EnabledIfEnvironmentVariable(named = "CI_POSTGRES_SMOKE", matches = "true")
class ApplicationStartupSmokeTest {

    private static final String USERNAME = "ci-platform-admin";
    private static final String PASSWORD = "CiStrongPassword-2026!";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ParoisseRepository paroisseRepository;
    @Autowired
    private TypeDemandeRepository typeDemandeRepository;
    @Autowired
    private ForfaitTarifRepository forfaitTarifRepository;
    @Autowired
    private HoraireRepository horaireRepository;
    @Autowired
    private TypePaiementRepository typePaiementRepository;
    @Autowired
    private TenantCatalogBootstrapService tenantCatalogBootstrapService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Test
    void storagePlacementFollowsNewParishTransaction() {
        var id=java.util.UUID.randomUUID();
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status->{
            jdbc.update("INSERT INTO paroisse(public_id,nom,adresse,doyenne_id,statut_tenant,is_active) SELECT ?, 'CI storage', 'Test', id, 'ACTIVE', TRUE FROM doyenne ORDER BY id LIMIT 1",id);
            assertThat(jdbc.queryForObject("SELECT shard_key FROM parish_storage_placement WHERE parish_id=?",String.class,id)).isEqualTo("primary");
            status.setRollbackOnly();return null;
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM paroisse WHERE public_id=?",Long.class,id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parish_storage_placement WHERE parish_id=?",Long.class,id)).isZero();
    }

    @BeforeEach
    void ensurePlatformUserExists() {
        if (!userRepository.existsByUsernameIgnoreCaseAndStatusDelFalse(USERNAME)) {
            User user = new User();
            user.setNom("Administrateur");
            user.setPrenom("CI");
            user.setUsername(USERNAME);
            user.setEmail("ci-admin@example.test");
            user.setPassword(passwordEncoder.encode(PASSWORD));
            user.setRole(UserRole.SUPER_ADMIN);
            user.setIsGlobal(true);
            user.setIsActive(true);
            user.setStatusDel(false);
            userRepository.save(user);
        }
    }


    @Test
    void publicDemandCanBeCreatedAndTrackedWithoutExposingClearContactData() throws Exception {
        Paroisse parish = paroisseRepository
                .findAllByStatusDelFalseAndIsSystemFalseOrderByNomAsc()
                .stream()
                .findFirst()
                .orElseThrow();
        parish.appliquerStatut(StatutTenant.ACTIVE);
        paroisseRepository.save(parish);
        tenantCatalogBootstrapService.seedDefaultsIfEmpty(parish);

        Horaire schedule = horaireRepository
                .findByParoisseAndIsActiveTrueAndStatusDelFalseAndDateSpecifiqueIsNull(parish)
                .stream()
                .filter(value -> value.getJourSemaine() != JourSemaine.DIMANCHE)
                .findFirst()
                .orElseThrow();

        TypeDemande requestType = typeDemandeRepository
                .findByParoisseAndStatusDelFalse(parish)
                .stream()
                .filter(value -> Boolean.TRUE.equals(value.getIsActive()))
                .filter(value -> value.getJoursCelebrationAutorises().isEmpty()
                        || value.getJoursCelebrationAutorises().contains(schedule.getJourSemaine()))
                .filter(value -> forfaitTarifRepository
                        .findByTypeDemandeAndIsActiveTrueAndStatusDelFalse(value)
                        .stream()
                        .anyMatch(price -> Integer.valueOf(1).equals(price.getNombreCelebration())
                                && price.getNatureForfait() == NatureForfaitEnum.NORMALE
                                && (price.getJoursCelebrationAutorises().isEmpty()
                                || price.getJoursCelebrationAutorises().contains(schedule.getJourSemaine()))))
                .findFirst()
                .orElseThrow();

        ForfaitTarif price = forfaitTarifRepository
                .findByTypeDemandeAndIsActiveTrueAndStatusDelFalse(requestType)
                .stream()
                .filter(value -> Integer.valueOf(1).equals(value.getNombreCelebration()))
                .filter(value -> value.getNatureForfait() == NatureForfaitEnum.NORMALE)
                .filter(value -> value.getJoursCelebrationAutorises().isEmpty()
                        || value.getJoursCelebrationAutorises().contains(schedule.getJourSemaine()))
                .findFirst()
                .orElseThrow();

        TypePaiement paymentType = typePaiementRepository
                .findByModeAndStatusDelFalse(ModePaiement.ESPECES)
                .orElseThrow();

        LocalDate celebrationDate = LocalDate.now().plusDays(30);
        while (JourSemaine.fromDayOfWeek(celebrationDate.getDayOfWeek()) != schedule.getJourSemaine()) {
            celebrationDate = celebrationDate.plusDays(1);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("intention", "Action de grâce pour le parcours automatisé");
        payload.put("nomFidele", "Testeur");
        payload.put("prenomFidele", "CI");
        payload.put("telFidele", "+22890123456");
        payload.put("emailFidele", "fidele-ci@example.test");
        payload.put("dateDebut", celebrationDate.toString());
        payload.put("joursConsecutifs", false);
        payload.put("paroissePublicId", parish.getPublicId());
        payload.put("typeDemandePublicId", requestType.getPublicId());
        payload.put("forfaitTarifPublicId", price.getPublicId());
        payload.put("horairePublicId", schedule.getPublicId());
        payload.put("typePaiementPublicId", paymentType.getPublicId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> creation = restTemplate.postForEntity(
                "/demandes",
                new HttpEntity<>(payload, headers),
                String.class
        );

        assertThat(creation.getStatusCode().value()).isEqualTo(201);
        JsonNode created = objectMapper.readTree(creation.getBody());
        String trackingCode = created.path("codeSuivie").asText();
        assertThat(trackingCode).matches("^MS-[A-Z0-9]+-[A-Z0-9]{10}$");
        assertThat(created.path("facturePublicId").asText()).isNotBlank();

        ResponseEntity<String> tracking =
                restTemplate.getForEntity("/demandes/code/" + trackingCode, String.class);

        assertThat(tracking.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(tracking.getBody())
                .contains(trackingCode)
                .doesNotContain("+22890123456")
                .doesNotContain("fidele-ci@example.test");

        ResponseEntity<String> phoneLookup = restTemplate.postForEntity(
                "/demandes/suivi/par-telephone",
                new HttpEntity<>(Map.of("telephone", "+22890123456"), headers),
                String.class
        );

        assertThat(phoneLookup.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode phoneLookupBody = objectMapper.readTree(phoneLookup.getBody());
        assertThat(phoneLookupBody.path("codes").toString()).contains(trackingCode);
        assertThat(phoneLookupBody.path("count").asInt()).isEqualTo(1);
        assertThat(phoneLookup.getBody())
                .doesNotContain("+22890123456")
                .doesNotContain("fidele-ci@example.test")
                .doesNotContain("Action de grâce")
                .doesNotContain("Testeur");

        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> login = restTemplate.postForEntity(
                "/auth/login",
                new HttpEntity<>(
                        Map.of("username", USERNAME, "password", PASSWORD),
                        loginHeaders
                ),
                String.class
        );
        String setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();

        HttpHeaders cashHeaders = new HttpHeaders();
        cashHeaders.set(HttpHeaders.COOKIE, setCookie.split(";", 2)[0]);
        cashHeaders.setOrigin("http://localhost:5173");
        cashHeaders.set("X-Requested-With", "XMLHttpRequest");
        cashHeaders.setContentType(MediaType.APPLICATION_JSON);
        String demandPublicId = created.path("publicId").asText();

        assertThat(created.path("natureForfait").asText()).isEqualTo("NORMALE");
        assertThat(created.path("validationRequise").asBoolean()).isFalse();
        assertThat(created.path("statutValidation").asText()).isEqualTo("VALIDEE");
        assertThat(created.path("statutDemande").asText()).isEqualTo("VALIDEE");
        assertThat(created.path("paiementDisponible").asBoolean()).isTrue();

        ResponseEntity<String> cashPayment = restTemplate.exchange(
                "/details-paiement/caisse/" + demandPublicId,
                HttpMethod.POST,
                new HttpEntity<>(cashHeaders),
                String.class
        );

        assertThat(cashPayment.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode paid = objectMapper.readTree(cashPayment.getBody());
        assertThat(paid.path("statutPaiement").asText()).isEqualTo("PAYE");
        assertThat(paid.path("modePaiement").asText()).isEqualTo("ESPECES");
        assertThat(paid.path("provider").asText()).isEqualTo("CAISSE_LOCALE");
        assertThat(paid.path("facturePublicId").asText())
                .isEqualTo(created.path("facturePublicId").asText());

        ResponseEntity<String> duplicateCashPayment = restTemplate.exchange(
                "/details-paiement/caisse/" + demandPublicId,
                HttpMethod.POST,
                new HttpEntity<>(cashHeaders),
                String.class
        );

        assertThat(duplicateCashPayment.getStatusCode().is4xxClientError()).isTrue();
        assertThat(duplicateCashPayment.getBody()).contains("déjà payée");

        ResponseEntity<String> paidTracking =
                restTemplate.getForEntity("/demandes/code/" + trackingCode, String.class);
        assertThat(paidTracking.getBody()).contains("\"statutPaiement\":\"PAYE\"");

        ResponseEntity<byte[]> receipt =
                restTemplate.getForEntity("/demandes/code/" + trackingCode + "/recu.pdf", byte[].class);
        assertThat(receipt.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(receipt.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(receipt.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("recu-" + trackingCode + ".pdf");
        assertThat(receipt.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .isEqualTo("no-store, private");
        assertThat(new String(receipt.getBody(), 0, 5, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");

        HttpHeaders sheetHeaders = new HttpHeaders();
        sheetHeaders.set(HttpHeaders.COOKIE, setCookie.split(";", 2)[0]);
        ResponseEntity<byte[]> celebrationSheet = restTemplate.exchange(
                "/celebrations/paroisse/" + parish.getPublicId()
                        + "/feuille.pdf?date=" + celebrationDate,
                HttpMethod.GET,
                new HttpEntity<>(sheetHeaders),
                byte[].class
        );
        assertThat(celebrationSheet.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(celebrationSheet.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(celebrationSheet.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("feuille-intentions-" + celebrationDate + ".pdf");
        assertThat(celebrationSheet.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .isEqualTo("no-store, private");
        assertThat(new String(celebrationSheet.getBody(), 0, 5, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");

        // Desktop offline replay: create once even after repeated/concurrent submissions.
        payload.put("telFidele", "+22890123457");
        String operationId = java.util.UUID.randomUUID().toString();
        Map<String,Object> operation = Map.of("operationId", operationId, "kind", "CREATE_DEMANDE", "payload", payload);
        var desktopCreated = restTemplate.postForEntity("/desktop/operations", new HttpEntity<>(operation,cashHeaders),String.class);
        assertThat(desktopCreated.getStatusCode().value()).isEqualTo(200);
        JsonNode desktopDemand=objectMapper.readTree(desktopCreated.getBody());
        assertThat(jdbc.queryForObject("SELECT shard_key FROM parish_storage_placement WHERE parish_id=?",String.class,parish.getPublicId())).isEqualTo("primary");
        org.assertj.core.api.Assertions.assertThatThrownBy(()->jdbc.update("UPDATE parish_storage_placement SET shard_key='unconfigured' WHERE parish_id=?",parish.getPublicId()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM paroisse p WHERE NOT EXISTS (SELECT 1 FROM parish_storage_placement s WHERE s.parish_id=p.public_id)",Long.class)).isZero();
        jdbc.update("DELETE FROM parish_storage_placement WHERE parish_id=?",parish.getPublicId());
        try {
            var unavailablePlacement=restTemplate.postForEntity("/desktop/operations",new HttpEntity<>(operation,cashHeaders),String.class);
            assertThat(unavailablePlacement.getStatusCode().value()).isEqualTo(503);
        } finally {
            jdbc.update("INSERT INTO parish_storage_placement(parish_id) VALUES (?)",parish.getPublicId());
        }
        var branding = restTemplate.exchange("/desktop/paroisses/"+parish.getPublicId()+"/receipt-branding",HttpMethod.GET,new HttpEntity<>(cashHeaders),String.class);
        assertThat(branding.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(branding.getBody()).path("nom").asText()).isEqualTo(parish.getNom());
        assertThat(branding.getBody()).doesNotContain("ibanOrRib", "nomBanque");
        var anonymousBranding=restTemplate.getForEntity("/desktop/paroisses/"+parish.getPublicId()+"/receipt-branding",String.class);
        assertThat(anonymousBranding.getStatusCode().is4xxClientError()).isTrue();
        String localReference="MSL-"+operationId.toUpperCase(java.util.Locale.ROOT);
        assertThat(desktopDemand.path("localReference").asText()).isEqualTo(localReference);
        var localTracking=restTemplate.getForEntity("/demandes/code/"+localReference.toLowerCase(java.util.Locale.ROOT),String.class);
        assertThat(localTracking.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(localTracking.getBody()).path("codeSuivie").asText()).isEqualTo(desktopDemand.path("codeSuivie").asText());
        // Simulate a V31 response: upgrade/replay backfills its reference without recreating the demand.
        jdbc.update("DELETE FROM desktop_tracking_reference WHERE reference=?",localReference);
        var legacy=(com.fasterxml.jackson.databind.node.ObjectNode)desktopDemand.deepCopy();legacy.remove("localReference");
        jdbc.update("UPDATE desktop_operation SET response=? WHERE operation_id=?",legacy.toString(),java.util.UUID.fromString(operationId));
        var replay=restTemplate.postForEntity("/desktop/operations", new HttpEntity<>(operation,cashHeaders),String.class);
        assertThat(replay.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(replay.getBody())).isEqualTo(desktopDemand);
        var changed=new LinkedHashMap<>(payload); changed.put("intention","Une autre intention après le premier envoi");
        var conflict=restTemplate.postForEntity("/desktop/operations",new HttpEntity<>(Map.of("operationId",operationId,"kind","CREATE_DEMANDE","payload",changed),cashHeaders),String.class);
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
        var anonymous=restTemplate.postForEntity("/desktop/operations",new HttpEntity<>(operation,headers),String.class);
        assertThat(anonymous.getStatusCode().value()).isEqualTo(401);

        String cashId=java.util.UUID.randomUUID().toString();
        var wrongCash=Map.of("operationId",cashId,"kind","CASH_PAYMENT","payload",Map.of("demandePublicId",desktopDemand.path("publicId").asText(),"expectedMontant",desktopDemand.path("montant").asInt()+1));
        var wrongAmount=restTemplate.postForEntity("/desktop/operations",new HttpEntity<>(wrongCash,cashHeaders),String.class);
        assertThat(wrongAmount.getStatusCode().value()).isEqualTo(409);
        var cashOperation=Map.of("operationId",cashId,"kind","CASH_PAYMENT","payload",Map.of("demandePublicId",desktopDemand.path("publicId").asText(),"expectedMontant",desktopDemand.path("montant").asInt()));
        // The rejected transaction was rolled back: the same ID can now be claimed successfully.
        var first=java.util.concurrent.CompletableFuture.supplyAsync(()->restTemplate.postForEntity("/desktop/operations",new HttpEntity<>(cashOperation,cashHeaders),String.class));
        var second=java.util.concurrent.CompletableFuture.supplyAsync(()->restTemplate.postForEntity("/desktop/operations",new HttpEntity<>(cashOperation,cashHeaders),String.class));
        var firstPaid=first.get(30,java.util.concurrent.TimeUnit.SECONDS);
        var secondPaid=second.get(30,java.util.concurrent.TimeUnit.SECONDS);
        assertThat(firstPaid.getStatusCode().value()).isEqualTo(200);
        assertThat(secondPaid.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(firstPaid.getBody()).path("statutPaiement").asText()).isEqualTo("PAYE");
        assertThat(objectMapper.readTree(secondPaid.getBody())).isEqualTo(objectMapper.readTree(firstPaid.getBody()));
        var localReceipt=restTemplate.getForEntity("/demandes/code/"+localReference+"/recu.pdf",byte[].class);
        assertThat(localReceipt.getStatusCode().value()).isEqualTo(200);
        assertThat(new String(localReceipt.getBody(),0,5,StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try(var pdf=new com.lowagie.text.pdf.PdfReader(localReceipt.getBody())) {
            assertThat(new com.lowagie.text.pdf.parser.PdfTextExtractor(pdf).getTextFromPage(1).replaceAll("\\s+", "")).contains(localReference);
        }
        var localInvoice=restTemplate.getForEntity("/facture/code-suivie/"+localReference,String.class);
        assertThat(localInvoice.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void applicationStartsAndExposesHealthyReadinessEndpoint() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/admin/users", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options"))
                .isEqualTo("nosniff");
    }

    @Test
    void loginUsesHttpOnlyCookieAndAuthenticatesMeEndpoint() {
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> loginRequest = new HttpEntity<>(
                Map.of("username", USERNAME, "password", PASSWORD),
                loginHeaders
        );

        ResponseEntity<String> loginResponse =
                restTemplate.postForEntity("/auth/login", loginRequest, String.class);

        assertThat(loginResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(loginResponse.getBody()).doesNotContain("eyJ");

        String setCookie = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .isNotBlank()
                .contains("MS_AT=")
                .contains("HttpOnly")
                .contains("SameSite=Lax");

        HttpHeaders authenticatedHeaders = new HttpHeaders();
        authenticatedHeaders.set(HttpHeaders.COOKIE, setCookie.split(";", 2)[0]);

        ResponseEntity<String> meResponse = restTemplate.exchange(
                "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(authenticatedHeaders),
                String.class
        );

        assertThat(meResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(meResponse.getBody())
                .contains(USERNAME)
                .contains("\"isGlobal\":true");
    }
}
