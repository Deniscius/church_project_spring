package com.eyram.dev.church_project_spring.service.billing;

import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.config.PlatformBillingProperties;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.entities.ParoisseAbonnement;
import com.eyram.dev.church_project_spring.entities.PlanSaas;
import com.eyram.dev.church_project_spring.enums.PlanAbonnement;
import com.eyram.dev.church_project_spring.repositories.ParoisseAbonnementRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.PlanSaasService;
import com.eyram.dev.church_project_spring.service.ProfessionalEmailService;
import com.eyram.dev.church_project_spring.service.payment.fedapay.FedaPayClient;
import com.eyram.dev.church_project_spring.service.tenant.TenantCatalogBootstrapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionBillingServicePricingTest {

    @Mock private ParoisseRepository paroisseRepository;
    @Mock private ParoisseAbonnementRepository abonnementRepository;
    @Mock private FedaPayClient fedaPayClient;
    @Mock private FedaPayProperties fedaPayProperties;
    @Mock private PlatformBillingProperties properties;
    @Mock private PlanSaasService planSaasService;
    @Mock private TenantCatalogBootstrapService tenantCatalogBootstrapService;
    @Mock private TenantAccessService tenantAccessService;
    @Mock private ProfessionalEmailService professionalEmailService;
    @Mock private ParoisseAccessRepository paroisseAccessRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private CacheManager cacheManager;

    private SubscriptionBillingService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionBillingService(
                paroisseRepository,
                abonnementRepository,
                fedaPayClient,
                fedaPayProperties,
                properties,
                planSaasService,
                tenantCatalogBootstrapService,
                tenantAccessService,
                professionalEmailService,
                paroisseAccessRepository,
                userRepository,
                transactionTemplate,
                cacheManager
        );
    }

    @Test
    void createPending_snapshotsCurrentPriceAndDuration() {
        PlanSaas pricing = new PlanSaas();
        pricing.setCode(PlanAbonnement.ANNUEL);
        pricing.setMontantXof(45_000);
        pricing.setDureeMois(12);

        Paroisse paroisse = new Paroisse();
        when(planSaasService.requireActive(PlanAbonnement.ANNUEL)).thenReturn(pricing);
        when(abonnementRepository.save(any(ParoisseAbonnement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParoisseAbonnement saved = service.createPending(paroisse, PlanAbonnement.ANNUEL);

        assertEquals(PlanAbonnement.ANNUEL, saved.getPlan());
        assertEquals(45_000, saved.getMontant());
        assertEquals(12, saved.getDureeMois());
        verify(abonnementRepository).save(saved);
    }
}
