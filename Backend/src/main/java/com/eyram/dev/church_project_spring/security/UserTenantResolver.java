package com.eyram.dev.church_project_spring.security;

import com.eyram.dev.church_project_spring.entities.ParoisseAccess;
import com.eyram.dev.church_project_spring.entities.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class UserTenantResolver {

    public Long resolveTenantId(User user) {

        // Un utilisateur global n'est pas limité à une paroisse.
        if (Boolean.TRUE.equals(user.getIsGlobal())) {
            return null;
        }

        List<ParoisseAccess> accesses =
                user.getParoisseAccesses() != null
                        ? user.getParoisseAccesses()
                        : Collections.emptyList();

        List<Long> tenantIds = accesses.stream()
                .filter(Objects::nonNull)
                .filter(access -> Boolean.TRUE.equals(access.getActive()))
                .filter(access -> !Boolean.TRUE.equals(access.getStatusDel()))
                .map(ParoisseAccess::getParoisse)
                .filter(Objects::nonNull)
                .map(paroisse -> paroisse.getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (tenantIds.isEmpty()) {
            throw new TenantAccessException(
                    "Aucune paroisse active n'est assignée à cet utilisateur"
            );
        }

        if (tenantIds.size() > 1) {
            throw new TenantAccessException(
                    "Plusieurs paroisses actives sont assignées à cet utilisateur"
            );
        }

        return tenantIds.get(0);
    }
}