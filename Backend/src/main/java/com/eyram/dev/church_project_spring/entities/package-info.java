
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = Long.class),
        defaultCondition = "tenant_id = :tenantId"
)
package com.eyram.dev.church_project_spring.entities;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;