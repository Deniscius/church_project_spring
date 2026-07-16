package com.eyram.dev.church_project_spring.context;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantFilterAspectTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter filter;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private TenantFilterAspect aspect;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void enablesAndCleansFilterInsideTenantTransaction() throws Throwable {
        TenantContext.setCurrentTenant(42L);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.getEnabledFilter("tenantFilter")).thenReturn(null);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.applyTenantFilter(joinPoint);

        assertEquals("result", result);
        verify(filter).setParameter("tenantId", 42L);
        verify(session).disableFilter("tenantFilter");
    }

    @Test
    void doesNotEnableFilterForGlobalContext() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.applyTenantFilter(joinPoint);

        assertEquals("result", result);
        verify(entityManager, never()).unwrap(Session.class);
    }

    @Test
    void cleansFilterWhenBusinessMethodFails() throws Throwable {
        TenantContext.setCurrentTenant(42L);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.getEnabledFilter("tenantFilter")).thenReturn(null);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("failure"));

        assertThrows(IllegalStateException.class, () -> aspect.applyTenantFilter(joinPoint));

        verify(session).disableFilter("tenantFilter");
    }

    @Test
    void leavesAnExistingFilterEnabledForNestedTransaction() throws Throwable {
        TenantContext.setCurrentTenant(42L);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.getEnabledFilter("tenantFilter")).thenReturn(filter);
        when(joinPoint.proceed()).thenReturn("result");

        aspect.applyTenantFilter(joinPoint);

        verify(filter).setParameter("tenantId", 42L);
        verify(session, never()).disableFilter("tenantFilter");
    }
}
