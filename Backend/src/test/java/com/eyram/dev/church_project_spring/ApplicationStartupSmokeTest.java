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
                "saspay.poll-delay-ms=3600000",
                "spring.flyway.repair-on-migrate=false"
        }
)
@EnabledIfEnvironmentVariable(named = "CI_POSTGRES_SMOKE", matches = "true")
class ApplicationStartupSmokeTest {

    private static final String USERNAME = "ci-platform-admin";
    private static final String PASSWORD = "CiStrongPassword-2026!";

    @Autowired private com.eyram.dev.church_project_spring.service.mail.MailOutbox mailOutbox;

    @Test
    void outboxInsertRollsBackWithBusinessTransaction() {
        long before=jdbc.queryForObject("SELECT count(*) FROM mail_outbox",Long.class);
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status->{
            mailOutbox.enqueue("rollback@example.test","Subject","Body");
            status.setRollbackOnly();return null;
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mail_outbox",Long.class)).isEqualTo(before);
    }

    @Test
    void syncCursorSerializesConcurrentCommits() throws Exception {
        long parish=jdbc.queryForObject("SELECT id FROM paroisse ORDER BY id LIMIT 1",Long.class);
        var first=java.util.UUID.randomUUID();var second=java.util.UUID.randomUUID();
        var held=new java.util.concurrent.CountDownLatch(1);var commit=new java.util.concurrent.CountDownLatch(1);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{
            var one=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status->{
                jdbc.queryForObject("SELECT desktop_record_demand_change(?,?)",String.class,parish,first);
                long cursor=jdbc.queryForObject("SELECT revision FROM desktop_sync_clock WHERE parish_id=?",Long.class,parish);
                held.countDown();try{if(!commit.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("test timeout");}catch(InterruptedException e){throw new IllegalStateException(e);}return cursor;
            }));
            assertThat(held.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var two=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status->{
                jdbc.queryForObject("SELECT desktop_record_demand_change(?,?)",String.class,parish,second);
                return jdbc.queryForObject("SELECT revision FROM desktop_sync_clock WHERE parish_id=?",Long.class,parish);
            }));
            org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.TimeoutException.class,()->two.get(150,java.util.concurrent.TimeUnit.MILLISECONDS));
            commit.countDown();
            assertThat(two.get(5,java.util.concurrent.TimeUnit.SECONDS)).isGreaterThan(one.get(5,java.util.concurrent.TimeUnit.SECONDS));
        }finally{
            commit.countDown();pool.shutdownNow();
            jdbc.update("DELETE FROM desktop_demand_change WHERE demand_public_id IN (?,?)",first,second);
        }
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void mailOutboxEncryptsRetriesAndErasesDeliveredPayload() throws Exception {
        mailOutbox.enqueue("outbox@example.test","Test","private-token-not-in-cleartext");
        var id=jdbc.queryForObject("SELECT id FROM mail_outbox ORDER BY created_at DESC LIMIT 1",java.util.UUID.class);
        byte[] payload=jdbc.queryForObject("SELECT payload FROM mail_outbox WHERE id=?",byte[].class,id);
        assertThat(new String(payload,StandardCharsets.ISO_8859_1)).doesNotContain("private-token","outbox@example.test");
        var first=mailOutbox.claim().stream().filter(m->m.id().equals(id)).findFirst().orElseThrow();
        assertThat(mailOutbox.decode(first).body()).isEqualTo("private-token-not-in-cleartext");
        var forged=new com.eyram.dev.church_project_spring.service.mail.MailOutbox.Claimed(java.util.UUID.randomUUID(),first.payload(),first.attempts(),first.token());
        org.junit.jupiter.api.Assertions.assertThrows(javax.crypto.AEADBadTagException.class,()->mailOutbox.decode(forged));
        assertThat(mailOutbox.claim()).noneMatch(m->m.id().equals(id));
        mailOutbox.failed(first,new IllegalStateException("sensitive-provider-error"));
        assertThat(jdbc.queryForObject("SELECT last_error FROM mail_outbox WHERE id=?",String.class,id)).isEqualTo("IllegalStateException");
        jdbc.update("UPDATE mail_outbox SET available_at=CURRENT_TIMESTAMP-INTERVAL '1 minute' WHERE id=?",id);
        var retry=mailOutbox.claim().stream().filter(m->m.id().equals(id)).findFirst().orElseThrow();
        assertThat(retry.attempts()).isEqualTo(2);
        mailOutbox.sent(first); // A previous lease cannot acknowledge a newer attempt.
        assertThat(jdbc.queryForObject("SELECT state FROM mail_outbox WHERE id=?",String.class,id)).isEqualTo("SENDING");
        mailOutbox.sent(retry);
        assertThat(jdbc.queryForObject("SELECT payload FROM mail_outbox WHERE id=?",byte[].class,id)).isNull();
    }

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
        new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                .execute(
                        status -> {
                            jdbc.update(
                                    "INSERT INTO"
                                        + " paroisse(public_id,nom,adresse,doyenne_id,statut_tenant,is_active)"
                                        + " SELECT ?, 'CI storage', 'Test', id, 'ACTIVE', TRUE FROM"
                                        + " doyenne ORDER BY id LIMIT 1",
                                    id);
                            assertThat(
                                            jdbc.queryForObject(
                                                    "SELECT shard_key FROM parish_storage_placement"
                                                        + " WHERE parish_id=?",
                                                    String.class,
                                                    id))
                                    .isEqualTo("primary");
                            status.setRollbackOnly();
                            return null;
                        });
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM paroisse WHERE public_id=?", Long.class, id))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM parish_storage_placement WHERE parish_id=?",
                                Long.class,
                                id))
                .isZero();
    }

    private void assertIncrementalSync(Paroisse parish,String demandId,HttpHeaders headers)throws Exception{
        String base="/desktop/paroisses/"+parish.getPublicId();
        var state=restTemplate.exchange(base+"/sync/state",HttpMethod.GET,new HttpEntity<>(headers),String.class);
        assertThat(state.getStatusCode().value()).isEqualTo(200);
        long cursor=objectMapper.readTree(state.getBody()).path("cursor").asLong();
        var changes=restTemplate.exchange(base+"/sync/changes?after=0",HttpMethod.GET,new HttpEntity<>(headers),String.class);
        assertThat(changes.getStatusCode().value()).isEqualTo(200);
        var feed=objectMapper.readTree(changes.getBody());
        assertThat(feed.path("upserts").findValuesAsText("publicId")).contains(demandId);
        assertThat(feed.path("upserts").findValuesAsText("statutPaiement")).contains("PAYE");
        var synthetic=new java.util.ArrayList<java.util.UUID>();
        try{
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status->{
                for(int i=0;i<101;i++){
                    var id=java.util.UUID.randomUUID();synthetic.add(id);
                    jdbc.queryForObject("SELECT desktop_record_demand_change(?,?)",String.class,parish.getId(),id);
                }
                return null;
            });
            var pageOne=objectMapper.readTree(restTemplate.exchange(base+"/sync/changes?after="+cursor,HttpMethod.GET,new HttpEntity<>(headers),String.class).getBody());
            assertThat(pageOne.path("deleted").size()).isEqualTo(100);
            assertThat(pageOne.path("hasMore").asBoolean()).isTrue();
            var pageTwo=objectMapper.readTree(restTemplate.exchange(base+"/sync/changes?after="+pageOne.path("cursor").asLong(),HttpMethod.GET,new HttpEntity<>(headers),String.class).getBody());
            assertThat(pageTwo.path("deleted").size()).isEqualTo(1);
            assertThat(pageTwo.path("hasMore").asBoolean()).isFalse();
        }finally{synthetic.forEach(id->jdbc.update("DELETE FROM desktop_demand_change WHERE demand_public_id=?",id));}
        try{
            jdbc.update("UPDATE demande SET status_del=true WHERE public_id=?",java.util.UUID.fromString(demandId));
            var removed=restTemplate.exchange(base+"/sync/changes?after="+cursor,HttpMethod.GET,new HttpEntity<>(headers),String.class);
            assertThat(removed.getStatusCode().value()).isEqualTo(200);
            assertThat(objectMapper.readTree(removed.getBody()).path("deleted").toString()).contains(demandId);
        }finally{jdbc.update("UPDATE demande SET status_del=false WHERE public_id=?",java.util.UUID.fromString(demandId));}
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(restTemplate.getRootUri()+base+"/events"))
            .header("Cookie",headers.getFirst(HttpHeaders.COOKIE)).header("Accept","text/event-stream").timeout(java.time.Duration.ofSeconds(10)).build();
        var response=java.net.http.HttpClient.newHttpClient().send(request,java.net.http.HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode()).isEqualTo(200);
        try(var reader=new java.io.BufferedReader(new java.io.InputStreamReader(response.body(),StandardCharsets.UTF_8))){
            var event=new StringBuilder();String line;
            while((line=reader.readLine())!=null&&!line.isEmpty())event.append(line).append('\n');
            assertThat(event.toString()).contains("event:revision","data:").doesNotContain("nomFidele");
        }
    }

    private void assertTenantObjectIsolation(Paroisse target,String demandId,HttpHeaders adminHeaders)throws Exception{
        // Real cookie-authenticated HTTP requests: knowing another tenant's UUID grants no access.
        var other=new Paroisse();
        other.setNom("CI IDOR isolated parish");
        other.setAdresse("Test");
        other.setDoyenne(target.getDoyenne());other.appliquerStatut(StatutTenant.ACTIVE);other=paroisseRepository.save(other);
        var user=new User();
        user.setNom("Local");
        user.setPrenom("IDOR");
        user.setUsername("ci-idor-secretary");
        user.setEmail("ci-idor@example.test");
        user.setPassword(passwordEncoder.encode(PASSWORD));user.setRole(UserRole.SECRETAIRE);user.setIsGlobal(false);user.setIsActive(true);user.setStatusDel(false);user=userRepository.save(user);
        jdbc.update(
                "INSERT INTO"
                    + " paroisse_access(public_id,user_id,paroisse_id,role_paroisse,active,status_del)"
                    + " VALUES (?,?,?,'SECRETAIRE',true,false)",
                java.util.UUID.randomUUID(),
                user.getId(),
                other.getId());
        var loginHeaders=new HttpHeaders();loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        var login =
                restTemplate.postForEntity(
                        "/auth/login",
                        new HttpEntity<>(
                                Map.of("username", "ci-idor-secretary", "password", PASSWORD),
                                loginHeaders),
                        String.class);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        var headers=new HttpHeaders();
        headers.set(
                HttpHeaders.COOKIE,
                login.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0]);
        headers.setOrigin("http://localhost:5173");
        headers.set("X-Requested-With", "XMLHttpRequest");
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Prime the target's real caches as an authorized user before the cross-tenant reads.
        for(String path:java.util.List.of("/demandes/paroisse/"+target.getPublicId()+"?page=0&size=20", "/demandes/paroisse/"+target.getPublicId()+"/stats", "/demandes/paroisse/"+target.getPublicId()+"/impayees-proches?page=0&size=10")){
            assertThat(restTemplate.exchange(path,HttpMethod.GET,new HttpEntity<>(adminHeaders),String.class).getStatusCode().value()).isEqualTo(200);
        }
        var allowed =
                restTemplate.exchange(
                        "/desktop/paroisses/" + other.getPublicId() + "/receipt-branding",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);
        assertThat(allowed.getStatusCode().value()).isEqualTo(200);
        var deniedBranding =
                restTemplate.exchange(
                        "/desktop/paroisses/" + target.getPublicId() + "/receipt-branding",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);
        assertThat(deniedBranding.getStatusCode().value()).isIn(403,404);assertThat(deniedBranding.getBody()).doesNotContain(target.getNom());
        var deniedRead =
                restTemplate.exchange(
                        "/demandes/" + demandId,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);
        assertThat(deniedRead.getStatusCode().value()).isIn(403,404);
        for(String path:java.util.List.of("/demandes/paroisse/"+target.getPublicId()+"?page=0&size=20", "/demandes/paroisse/"+target.getPublicId()+"/stats", "/demandes/paroisse/"+target.getPublicId()+"/impayees-proches?page=0&size=10")){
            assertThat(restTemplate.exchange(path,HttpMethod.GET,new HttpEntity<>(headers),String.class).getStatusCode().value()).isIn(403,404);
        }
        assertThat(
                        restTemplate
                                .exchange(
                                        "/desktop/paroisses/" + target.getPublicId() + "/revision",
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers),
                                        String.class)
                                .getStatusCode()
                                .value())
                .isIn(403, 404);
        var deniedWrite =
                restTemplate.exchange("/desktop/paroisses/"+target.getPublicId()+"/sync/changes?after=0",HttpMethod.GET,new HttpEntity<>(headers),String.class);
        assertThat(deniedWrite.getStatusCode().value()).isIn(403,404);
        assertThat(restTemplate.exchange("/desktop/paroisses/"+target.getPublicId()+"/sync/state",HttpMethod.GET,new HttpEntity<>(headers),String.class).getStatusCode().value()).isIn(403,404);
        assertThat(restTemplate.exchange("/desktop/paroisses/"+target.getPublicId()+"/events",HttpMethod.GET,new HttpEntity<>(headers),String.class).getStatusCode().value()).isIn(403,404);
        deniedWrite =
                restTemplate.exchange(
                        "/demandes/" + demandId + "/intention",
                        HttpMethod.PATCH,
                        new HttpEntity<>(
                                Map.of(
                                        "intention",
                                        "Tentative de modification par une autre paroisse"),
                                headers),
                        String.class);
        assertThat(deniedWrite.getStatusCode().value()).isIn(403,404);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT intention FROM demande WHERE public_id=?",
                                String.class,
                                java.util.UUID.fromString(demandId)))
                .isEqualTo("Action de grâce pour le parcours automatisé");
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
        assertTenantObjectIsolation(parish, demandPublicId,cashHeaders);

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
        assertIncrementalSync(parish,demandPublicId,cashHeaders);
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
                restTemplate.getForEntity(
                        "/demandes/code/" + trackingCode + "/recu.pdf", byte[].class);
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
        Map<String, Object> operation =
                Map.of("operationId", operationId, "kind", "CREATE_DEMANDE", "payload", payload);
        var desktopCreated =
                restTemplate.postForEntity(
                        "/desktop/operations",
                        new HttpEntity<>(operation, cashHeaders),
                        String.class);
        assertThat(desktopCreated.getStatusCode().value()).isEqualTo(200);
        JsonNode desktopDemand=objectMapper.readTree(desktopCreated.getBody());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT shard_key FROM parish_storage_placement WHERE parish_id=?",
                                String.class,
                                parish.getPublicId()))
                .isEqualTo("primary");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE parish_storage_placement SET"
                                            + " shard_key='unconfigured' WHERE parish_id=?",
                                        parish.getPublicId()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM paroisse p WHERE NOT EXISTS (SELECT 1 FROM"
                                    + " parish_storage_placement s WHERE s.parish_id=p.public_id)",
                                Long.class))
                .isZero();
        jdbc.update("DELETE FROM parish_storage_placement WHERE parish_id=?",parish.getPublicId());
        try {
            var unavailablePlacement =
                    restTemplate.postForEntity(
                            "/desktop/operations",
                            new HttpEntity<>(operation, cashHeaders),
                            String.class);
            assertThat(unavailablePlacement.getStatusCode().value()).isEqualTo(503);
        } finally {
            jdbc.update(
                    "INSERT INTO parish_storage_placement(parish_id) VALUES (?)",
                    parish.getPublicId());
        }
        var branding =
                restTemplate.exchange(
                        "/desktop/paroisses/" + parish.getPublicId() + "/receipt-branding",
                        HttpMethod.GET,
                        new HttpEntity<>(cashHeaders),
                        String.class);
        assertThat(branding.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(branding.getBody()).path("nom").asText())
                .isEqualTo(parish.getNom());
        assertThat(branding.getBody()).doesNotContain("ibanOrRib", "nomBanque");
        String revisionUrl="/desktop/paroisses/"+parish.getPublicId()+"/revision";
        var revision=restTemplate.exchange(revisionUrl,HttpMethod.GET,new HttpEntity<>(cashHeaders),String.class);
        assertThat(revision.getStatusCode().value()).isEqualTo(200);
        String revisionValue = objectMapper.readTree(revision.getBody()).path("revision").asText();
        assertThat(revisionValue).matches("[a-f0-9]{64}");
        var conditional=new HttpHeaders();conditional.putAll(cashHeaders);conditional.setIfNoneMatch(revision.getHeaders().getETag());
        assertThat(restTemplate.exchange(revisionUrl,HttpMethod.GET,new HttpEntity<>(conditional),String.class).getStatusCode().value()).isEqualTo(304);
        jdbc.update(
                "UPDATE horaire SET updated_at=CURRENT_TIMESTAMP WHERE paroisse_id=?",
                parish.getId());
        var revised=restTemplate.exchange(revisionUrl,HttpMethod.GET,new HttpEntity<>(conditional),String.class);
        assertThat(revised.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(revised.getBody()).path("revision").asText())
                .isNotEqualTo(revisionValue);
        var anonymousBranding =
                restTemplate.getForEntity(
                        "/desktop/paroisses/" + parish.getPublicId() + "/receipt-branding",
                        String.class);
        assertThat(anonymousBranding.getStatusCode().is4xxClientError()).isTrue();
        String localReference="MSL-"+operationId.toUpperCase(java.util.Locale.ROOT);
        assertThat(desktopDemand.path("localReference").asText()).isEqualTo(localReference);
        var localTracking =
                restTemplate.getForEntity(
                        "/demandes/code/" + localReference.toLowerCase(java.util.Locale.ROOT),
                        String.class);
        assertThat(localTracking.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(localTracking.getBody()).path("codeSuivie").asText())
                .isEqualTo(desktopDemand.path("codeSuivie").asText());
        // Simulate a V31 response: upgrade/replay backfills its reference without recreating the demand.
        jdbc.update("DELETE FROM desktop_tracking_reference WHERE reference=?",localReference);
        var legacy=(com.fasterxml.jackson.databind.node.ObjectNode)desktopDemand.deepCopy();
        legacy.remove("localReference");
        jdbc.update(
                "UPDATE desktop_operation SET response=? WHERE operation_id=?",
                legacy.toString(),
                java.util.UUID.fromString(operationId));
        var replay =
                restTemplate.postForEntity(
                        "/desktop/operations",
                        new HttpEntity<>(operation, cashHeaders),
                        String.class);
        assertThat(replay.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(replay.getBody())).isEqualTo(desktopDemand);
        User colleague=new User();
        colleague.setNom("Collègue");
        colleague.setPrenom("CI");
        colleague.setUsername("ci-colleague");
        colleague.setEmail("ci-colleague@example.test");
        colleague.setPassword(passwordEncoder.encode(PASSWORD));colleague.setRole(UserRole.SUPER_ADMIN);colleague.setIsGlobal(true);colleague.setIsActive(true);colleague.setStatusDel(false);userRepository.save(colleague);
        var colleagueLogin =
                restTemplate.postForEntity(
                        "/auth/login",
                        new HttpEntity<>(
                                Map.of("username", "ci-colleague", "password", PASSWORD),
                                loginHeaders),
                        String.class);
        HttpHeaders colleagueHeaders=new HttpHeaders();colleagueHeaders.putAll(cashHeaders);
        colleagueHeaders.set(
                HttpHeaders.COOKIE,
                colleagueLogin.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0]);
        var colleagueReplay =
                restTemplate.postForEntity(
                        "/desktop/operations",
                        new HttpEntity<>(operation, colleagueHeaders),
                        String.class);
        assertThat(colleagueReplay.getStatusCode().value()).isEqualTo(200);assertThat(objectMapper.readTree(colleagueReplay.getBody())).isEqualTo(desktopDemand);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM desktop_operation WHERE parish_id=? AND"
                                    + " operation_id=?",
                                Long.class,
                                parish.getPublicId(),
                                java.util.UUID.fromString(operationId)))
                .isEqualTo(1L);
        var changed=new LinkedHashMap<>(payload);
        changed.put("intention", "Une autre intention après le premier envoi");
        var conflict =
                restTemplate.postForEntity(
                        "/desktop/operations",
                        new HttpEntity<>(
                                Map.of(
                                        "operationId",
                                        operationId,
                                        "kind",
                                        "CREATE_DEMANDE",
                                        "payload",
                                        changed),
                                cashHeaders),
                        String.class);
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
        var anonymous =
                restTemplate.postForEntity(
                        "/desktop/operations", new HttpEntity<>(operation, headers), String.class);
        assertThat(anonymous.getStatusCode().value()).isEqualTo(401);

        String cashId=java.util.UUID.randomUUID().toString();
        var wrongCash =
                Map.of(
                        "operationId",
                        cashId,
                        "kind",
                        "CASH_PAYMENT",
                        "payload",
                        Map.of(
                                "demandePublicId",
                                desktopDemand.path("publicId").asText(),
                                "expectedMontant",
                                desktopDemand.path("montant").asInt() + 1));
        var wrongAmount =
                restTemplate.postForEntity(
                        "/desktop/operations",
                        new HttpEntity<>(wrongCash, cashHeaders),
                        String.class);
        assertThat(wrongAmount.getStatusCode().value()).isEqualTo(409);
        var cashOperation =
                Map.of(
                        "operationId",
                        cashId,
                        "kind",
                        "CASH_PAYMENT",
                        "payload",
                        Map.of(
                                "demandePublicId",
                                desktopDemand.path("publicId").asText(),
                                "expectedMontant",
                                desktopDemand.path("montant").asInt()));
        // The rejected transaction was rolled back: the same ID can now be claimed successfully.
        var first =
                java.util.concurrent.CompletableFuture.supplyAsync(
                        () ->
                                restTemplate.postForEntity(
                                        "/desktop/operations",
                                        new HttpEntity<>(cashOperation, cashHeaders),
                                        String.class));
        var second =
                java.util.concurrent.CompletableFuture.supplyAsync(
                        () ->
                                restTemplate.postForEntity(
                                        "/desktop/operations",
                                        new HttpEntity<>(cashOperation, cashHeaders),
                                        String.class));
        var firstPaid=first.get(30,java.util.concurrent.TimeUnit.SECONDS);
        var secondPaid=second.get(30,java.util.concurrent.TimeUnit.SECONDS);
        assertThat(firstPaid.getStatusCode().value()).isEqualTo(200);
        assertThat(secondPaid.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(firstPaid.getBody()).path("statutPaiement").asText())
                .isEqualTo("PAYE");
        assertThat(objectMapper.readTree(secondPaid.getBody())).isEqualTo(objectMapper.readTree(firstPaid.getBody()));
        var colleagueCashReplay =
                restTemplate.postForEntity(
                        "/desktop/operations",
                        new HttpEntity<>(cashOperation, colleagueHeaders),
                        String.class);
        assertThat(colleagueCashReplay.getStatusCode().value()).isEqualTo(200);assertThat(objectMapper.readTree(colleagueCashReplay.getBody())).isEqualTo(objectMapper.readTree(firstPaid.getBody()));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM desktop_operation WHERE parish_id=? AND"
                                    + " operation_id=?",
                                Long.class,
                                parish.getPublicId(),
                                java.util.UUID.fromString(cashId)))
                .isEqualTo(1L);
        var localReceipt =
                restTemplate.getForEntity(
                        "/demandes/code/" + localReference + "/recu.pdf", byte[].class);
        assertThat(localReceipt.getStatusCode().value()).isEqualTo(200);
        assertThat(new String(localReceipt.getBody(), 0, 5, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");
        try(var pdf=new com.lowagie.text.pdf.PdfReader(localReceipt.getBody())) {
            assertThat(
                            new com.lowagie.text.pdf.parser.PdfTextExtractor(pdf)
                                    .getTextFromPage(1)
                                    .replaceAll("\\s+", ""))
                    .contains(localReference);
        }
        var localInvoice =
                restTemplate.getForEntity("/facture/code-suivie/" + localReference, String.class);
        assertThat(localInvoice.getStatusCode().value()).isEqualTo(200);
        assertSasPayHostedCheckout(payload,headers,cashHeaders,parish);
    }

    @Autowired private com.eyram.dev.church_project_spring.config.SasPayProperties sasPayProperties;

    @Autowired
    private com.eyram.dev.church_project_spring.service.payment.saspay.SasPayCheckoutStore
            sasPayStore;

    @Autowired
    private com.eyram.dev.church_project_spring.service.payment.saspay.SasPayPaymentService
            sasPayPayments;

    private void assertSasPayHostedCheckout(
            Map<String, Object> original,
            HttpHeaders publicHeaders,
            HttpHeaders adminHeaders,
            Paroisse parish)
            throws Exception {
        var posted = new java.util.concurrent.ConcurrentHashMap<String, JsonNode>();
        var transactionSessions = new java.util.concurrent.ConcurrentHashMap<String, String>();
        var paid = new java.util.concurrent.atomic.AtomicBoolean(false);
        var wrongCurrency = new java.util.concurrent.atomic.AtomicBoolean(false);
        var creations = new java.util.concurrent.atomic.AtomicInteger();
        var creationAttempts = new java.util.concurrent.atomic.AtomicInteger();
        var failCreation = new java.util.concurrent.atomic.AtomicBoolean(false);
        var server =
                com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/api/v1/",
                exchange -> {
                    assertThat(exchange.getRequestHeaders().getFirst("Authorization"))
                            .isEqualTo("Bearer sk_test_integration");
                    var response = objectMapper.createObjectNode();
                    int status = 200;
                    String path = exchange.getRequestURI().getPath();
                    if (path.equals("/api/v1/checkout-sessions/")
                            && exchange.getRequestMethod().equals("POST")) {
                        creationAttempts.incrementAndGet();
                        if (failCreation.get()) {
                            byte[] failure = "{}".getBytes(StandardCharsets.UTF_8);
                            exchange.getResponseHeaders().set("Content-Type", "application/json");
                            exchange.sendResponseHeaders(503, failure.length);
                            exchange.getResponseBody().write(failure);
                            exchange.close();
                            return;
                        }
                        String id = java.util.UUID.randomUUID().toString();
                        var request = objectMapper.readTree(exchange.getRequestBody());
                        assertThat(request.path("amount").isTextual()).isTrue();
                        assertThat(request.path("fee_charge_mode").asText()).isEqualTo("ADD_ON");
                        assertThat(request.path("metadata").path("missanye_reference").asText())
                                .isNotBlank();
                        posted.put(id, request);
                        creations.incrementAndGet();
                        response.put("id", id)
                                .put("checkout_url", "https://pay.saspay.me/checkout/" + id);
                        status = 201;
                    } else if (path.contains("checkout-sessions/") && path.endsWith("/status/")) {
                        String id = path.split("/")[4];
                        String tx =
                                transactionSessions.entrySet().stream()
                                        .filter(e -> e.getValue().equals(id))
                                        .map(Map.Entry::getKey)
                                        .findFirst()
                                        .orElseGet(
                                                () -> {
                                                    String key =
                                                            java.util.UUID.randomUUID().toString();
                                                    transactionSessions.put(key, id);
                                                    return key;
                                                });
                        response.put("id", id)
                                .put("status", paid.get() ? "PAID" : "PENDING")
                                .put("transaction_status", paid.get() ? "SUCCESS" : "PENDING")
                                .put("transaction_id", tx);
                    } else if (path.contains("payments/") && path.endsWith("/verify/")) {
                        String id = path.split("/")[4];
                        var request = posted.get(transactionSessions.get(id));
                        var amount = new java.math.BigDecimal(request.path("amount").asText());
                        response.put("id", id)
                                .put("status", "SUCCESS")
                                .put("transaction_type", "PAIEMENT")
                                .put("flow_direction", "INBOUND")
                                .put("currency", wrongCurrency.get() ? "EUR" : "XOF")
                                .put("requested_amount", amount.toPlainString())
                                .put("net_amount", amount.toPlainString())
                                .put("client_fee", "37.50")
                                .put(
                                        "debited_amount",
                                        amount.add(new java.math.BigDecimal("37.50"))
                                                .toPlainString())
                                .put("fee_charge_mode", "ADD_ON");
                    } else {
                        status = 404;
                    }
                    byte[] bytes =
                            objectMapper.writeValueAsBytes(
                                    Map.of("success", true, "data", response));
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
        server.start();
        String previousUrl = sasPayProperties.getApiBaseUrl();
        try {
            sasPayProperties.setApiBaseUrl(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1");
            sasPayProperties.setApiKey("sk_test_integration");
            sasPayProperties.setWebhookSecret("integration-signing-secret");
            sasPayProperties.setEnabled(true);
            var payload = new LinkedHashMap<>(original);
            payload.put(
                    "typePaiementPublicId",
                    typePaiementRepository
                            .findByModeAndStatusDelFalse(ModePaiement.FLOOZ)
                            .orElseThrow()
                            .getPublicId());
            payload.put("intention", "Paiement SasPay simulé pour vérifier les encaissements");
            payload.remove("emailFidele");
            var created =
                    restTemplate.postForEntity(
                            "/demandes", new HttpEntity<>(payload, publicHeaders), String.class);
            assertThat(created.getStatusCode().value()).isEqualTo(201);
            var demand = objectMapper.readTree(created.getBody());
            String code = demand.path("codeSuivie").asText();
            String endpoint = "/paiements/checkout/" + code;
            var quote = restTemplate.getForEntity("/paiements/quote/" + code, String.class);
            assertThat(
                            objectMapper
                                    .readTree(quote.getBody())
                                    .path("providerFeesPending")
                                    .asBoolean())
                    .isTrue();
            assertThat(
                            restTemplate
                                    .postForEntity(
                                            endpoint,
                                            new HttpEntity<>(Map.of(), publicHeaders),
                                            String.class)
                                    .getStatusCode()
                                    .value())
                    .isEqualTo(409);
            var createOne =
                    java.util.concurrent.CompletableFuture.supplyAsync(
                            () ->
                                    restTemplate.postForEntity(
                                            endpoint,
                                            new HttpEntity<>(
                                                    Map.of("email", "payeur-ci@example.test"),
                                                    publicHeaders),
                                            String.class));
            var createTwo =
                    java.util.concurrent.CompletableFuture.supplyAsync(
                            () ->
                                    restTemplate.postForEntity(
                                            endpoint,
                                            new HttpEntity<>(
                                                    Map.of("email", "payeur-ci@example.test"),
                                                    publicHeaders),
                                            String.class));
            var opened = createOne.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(
                            createTwo
                                    .get(20, java.util.concurrent.TimeUnit.SECONDS)
                                    .getStatusCode()
                                    .value())
                    .isEqualTo(200);
            assertThat(opened.getStatusCode().value()).isEqualTo(200);
            var checkout = objectMapper.readTree(opened.getBody());
            assertThat(checkout.path("paymentUrl").asText())
                    .startsWith("https://pay.saspay.me/checkout/");
            assertThat(opened.getBody()).doesNotContain("sk_test", "integration-signing-secret");
            var reused =
                    restTemplate.postForEntity(
                            endpoint, new HttpEntity<>(Map.of(), publicHeaders), String.class);
            assertThat(reused.getStatusCode().value()).isEqualTo(200);
            assertThat(creations.get()).isEqualTo(1);
            assertThat(
                            restTemplate
                                    .postForEntity(
                                            "/details-paiement/caisse/"
                                                    + demand.path("publicId").asText(),
                                            new HttpEntity<>(Map.of(), adminHeaders),
                                            String.class)
                                    .getStatusCode()
                                    .value())
                    .isEqualTo(409);
            paid.set(true);
            wrongCurrency.set(true);
            var rejected =
                    restTemplate.postForEntity(
                            "/paiements/reconcile/" + code,
                            new HttpEntity<>(Map.of(), publicHeaders),
                            String.class);
            assertThat(rejected.getStatusCode().is4xxClientError()).isTrue();
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT statut_paiement FROM demande WHERE code_suivie=?",
                                    String.class,
                                    code))
                    .isEqualTo("EN_ATTENTE");
            wrongCurrency.set(false);
            var firstConfirm =
                    java.util.concurrent.CompletableFuture.supplyAsync(
                            () ->
                                    restTemplate.postForEntity(
                                            "/paiements/reconcile/" + code,
                                            new HttpEntity<>(Map.of(), publicHeaders),
                                            String.class));
            var secondConfirm =
                    java.util.concurrent.CompletableFuture.supplyAsync(
                            () ->
                                    restTemplate.postForEntity(
                                            "/paiements/reconcile/" + code,
                                            new HttpEntity<>(Map.of(), publicHeaders),
                                            String.class));
            var confirmed = firstConfirm.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(
                            secondConfirm
                                    .get(20, java.util.concurrent.TimeUnit.SECONDS)
                                    .getStatusCode()
                                    .value())
                    .isEqualTo(200);
            assertThat(confirmed.getStatusCode().value()).isEqualTo(200);
            assertThat(objectMapper.readTree(confirmed.getBody()).path("statutPaiement").asText())
                    .isEqualTo("PAYE");
            String tx = transactionSessions.keySet().iterator().next();
            var event =
                    objectMapper.writeValueAsBytes(
                            Map.of("event", "transaction.success", "data", Map.of("id", tx)));
            String timestamp = "" + java.time.Instant.now().getEpochSecond();
            var webhookHeaders = new HttpHeaders();
            webhookHeaders.setContentType(MediaType.APPLICATION_JSON);
            webhookHeaders.set("X-Webhook-Event", "transaction.success");
            webhookHeaders.set("X-Webhook-Timestamp", timestamp);
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(
                    new javax.crypto.spec.SecretKeySpec(
                            "integration-signing-secret".getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            webhookHeaders.set(
                    "X-Webhook-Signature", java.util.HexFormat.of().formatHex(mac.doFinal(event)));
            for (int i = 0; i < 2; i++)
                assertThat(
                                restTemplate
                                        .postForEntity(
                                                "/webhooks/saspay",
                                                new HttpEntity<>(event, webhookHeaders),
                                                String.class)
                                        .getStatusCode()
                                        .value())
                        .isEqualTo(200);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM saspay_webhook_event WHERE"
                                        + " transaction_id=?",
                                    Long.class,
                                    java.util.UUID.fromString(tx)))
                    .isEqualTo(1L);
            webhookHeaders.set("X-Webhook-Timestamp", "1");
            assertThat(
                            restTemplate
                                    .postForEntity(
                                            "/webhooks/saspay",
                                            new HttpEntity<>(event, webhookHeaders),
                                            String.class)
                                    .getStatusCode()
                                    .value())
                    .isEqualTo(401);
            restTemplate.postForEntity(
                    "/paiements/reconcile/" + code,
                    new HttpEntity<>(Map.of(), publicHeaders),
                    String.class);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM ecriture_comptable WHERE"
                                        + " reference_externe=?",
                                    Long.class,
                                    "SASPAY:" + tx))
                    .isEqualTo(1L);
            var row =
                    jdbc.queryForMap(
                            "SELECT provider,provider_charged,provider_net,provider_fee FROM"
                                + " details_paiement dp JOIN facture f ON f.id=dp.facture_id JOIN"
                                + " demande d ON d.id=f.demande_id WHERE d.code_suivie=?",
                            code);
            assertThat(row.get("provider")).isEqualTo("SASPAY");
            assertThat((java.math.BigDecimal) row.get("provider_fee"))
                    .isEqualByComparingTo("37.50");
            var invoice = restTemplate.getForEntity("/facture/code-suivie/" + code, String.class);
            assertThat(
                            objectMapper
                                    .readTree(invoice.getBody())
                                    .path("reglement")
                                    .path("montantCharge")
                                    .decimalValue())
                    .isEqualByComparingTo((java.math.BigDecimal) row.get("provider_charged"));
            // Subscription creation uses SasPay too; repeated calls reuse the same checkout.
            assertThat(
                            restTemplate
                                    .postForEntity(
                                            "/comptabilite/abonnements/checkout/"
                                                    + parish.getPublicId(),
                                            new HttpEntity<>(Map.of(), adminHeaders),
                                            String.class)
                                    .getStatusCode()
                                    .value())
                    .isEqualTo(403);
            var accountant = new User();
            accountant.setNom("Comptable");
            accountant.setPrenom("SasPay CI");
            accountant.setUsername("ci-saspay-accountant");
            accountant.setEmail("saspay-accountant@example.test");
            accountant.setPassword(passwordEncoder.encode(PASSWORD));
            accountant.setRole(UserRole.COMPTABLE);
            accountant.setIsGlobal(true);
            accountant.setIsActive(true);
            accountant.setStatusDel(false);
            userRepository.save(accountant);
            var login =
                    restTemplate.postForEntity(
                            "/auth/login",
                            new HttpEntity<>(
                                    Map.of(
                                            "username",
                                            "ci-saspay-accountant",
                                            "password",
                                            PASSWORD),
                                    publicHeaders),
                            String.class);
            assertThat(login.getStatusCode().value()).isEqualTo(200);
            var financeHeaders = new HttpHeaders();
            financeHeaders.putAll(adminHeaders);
            financeHeaders.set(
                    HttpHeaders.COOKIE,
                    login.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0]);
            parish.setEmail("ci-saspay-parish@missanye.com");
            paroisseRepository.save(parish);
            paid.set(false);
            var sub =
                    restTemplate.postForEntity(
                            "/comptabilite/abonnements/checkout/" + parish.getPublicId(),
                            new HttpEntity<>(Map.of(), financeHeaders),
                            String.class);
            assertThat(sub.getStatusCode().value()).as(sub.getBody()).isEqualTo(200);
            var subscription = objectMapper.readTree(sub.getBody());
            assertThat(subscription.path("providerTransactionId").asText()).startsWith("SASPAY:");
            var repeat =
                    restTemplate.postForEntity(
                            "/comptabilite/abonnements/checkout/" + parish.getPublicId(),
                            new HttpEntity<>(Map.of(), financeHeaders),
                            String.class);
            assertThat(repeat.getStatusCode().value()).isEqualTo(200);
            assertThat(creations.get()).isEqualTo(2);
            var local =
                    sasPayStore.latest(
                            "ABONNEMENT",
                            java.util.UUID.fromString(
                                    subscription.path("abonnementPublicId").asText()));
            paid.set(true);
            sasPayPayments.refresh(local);
            var expiry =
                    jdbc.queryForObject(
                            "SELECT subscription_expires_at FROM paroisse WHERE public_id=?",
                            java.sql.Timestamp.class,
                            parish.getPublicId());
            sasPayPayments.refresh(local);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT subscription_expires_at FROM paroisse WHERE"
                                        + " public_id=?",
                                    java.sql.Timestamp.class,
                                    parish.getPublicId()))
                    .isEqualTo(expiry);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT activation_source FROM paroisse_abonnement WHERE"
                                        + " public_id=?",
                                    String.class,
                                    local.target()))
                    .isEqualTo("SASPAY");
            failCreation.set(true);
            var target = java.util.UUID.randomUUID();
            var txManager =
                    new org.springframework.transaction.support.TransactionTemplate(
                            transactionManager);
            var ambiguous =
                    txManager.execute(
                            t ->
                                    sasPayStore.start(
                                            "ABONNEMENT",
                                            target,
                                            2500,
                                            0,
                                            new LinkedHashMap<>(
                                                    Map.of(
                                                            "amount",
                                                            "2500.00",
                                                            "currency",
                                                            "XOF"))));
            assertThat(ambiguous.state()).isEqualTo("UNCERTAIN");
            int attempts = creationAttempts.get();
            var preserved =
                    txManager.execute(
                            t ->
                                    sasPayStore.start(
                                            "ABONNEMENT",
                                            target,
                                            2500,
                                            0,
                                            new LinkedHashMap<>(
                                                    Map.of(
                                                            "amount",
                                                            "2500.00",
                                                            "currency",
                                                            "XOF"))));
            assertThat(preserved.id()).isEqualTo(ambiguous.id());
            assertThat(creationAttempts.get()).isEqualTo(attempts);

        } finally {
            sasPayProperties.setEnabled(false);
            sasPayProperties.setApiKey("");
            sasPayProperties.setWebhookSecret("");
            sasPayProperties.setApiBaseUrl(previousUrl);
            server.stop(0);
        }
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
