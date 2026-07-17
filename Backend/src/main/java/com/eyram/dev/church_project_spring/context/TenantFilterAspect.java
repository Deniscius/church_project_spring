package com.eyram.dev.church_project_spring.context;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantFilterAspect {

    private static final String FILTER_NAME = "tenantFilter";
    private static final String TENANT_PARAMETER = "tenantId";

    @PersistenceContext
    private EntityManager entityManager;

    @Around("@within(org.springframework.transaction.annotation.Transactional) || "
            + "@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object applyTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return joinPoint.proceed();
        }

        Session session = entityManager.unwrap(Session.class);
        Filter existingFilter = session.getEnabledFilter(FILTER_NAME);
        boolean enabledHere = existingFilter == null;
        Filter filter = enabledHere ? session.enableFilter(FILTER_NAME) : existingFilter;
        filter.setParameter(TENANT_PARAMETER, tenantId);

        try {
            return joinPoint.proceed();
        } finally {
            if (enabledHere) {
                session.disableFilter(FILTER_NAME);
            }
        }
    }
}
