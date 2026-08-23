package com.eyram.dev.church_project_spring.service.billing;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.eyram.dev.church_project_spring.config.FedaPayProperties;
import com.eyram.dev.church_project_spring.config.PlatformBillingProperties;
import com.eyram.dev.church_project_spring.entities.Paroisse;
import com.eyram.dev.church_project_spring.repositories.ParoisseAbonnementRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseAccessRepository;
import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.repositories.UserRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.service.PlanSaasService;
import com.eyram.dev.church_project_spring.service.ProfessionalEmailService;
import com.eyram.dev.church_project_spring.service.payment.fedapay.FedaPayClient;
import com.eyram.dev.church_project_spring.service.tenant.TenantCatalogBootstrapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class SubscriptionBillingServiceTest {

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

    @Test
    void checkoutRejectsAnotherParishBeforeCreatingSubscription() {
        UUID parishId = UUID.randomUUID();
        Paroisse otherParish = new Paroisse();
        otherParish.setPublicId(parishId);

        when(paroisseRepository.findByPublicIdAndStatusDelFalse(parishId))
                .thenReturn(Optional.of(otherParish));
        org.mockito.Mockito.doThrow(new AccessDeniedException("Accès refusé"))
                .when(tenantAccessService).checkParoisseAccess(otherParish);
        org.mockito.Mockito.doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        }).when(transactionTemplate).execute(any());

        SubscriptionBillingService service = new SubscriptionBillingService(
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

        assertThrows(AccessDeniedException.class, () -> service.checkout(parishId, null));
        verify(abonnementRepository, never()).save(any());
        verify(fedaPayClient, never()).createTransaction(any());
    }
}
