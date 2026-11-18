package com.eyram.dev.church_project_spring.service.desktop;

import com.eyram.dev.church_project_spring.repositories.ParoisseRepository;
import com.eyram.dev.church_project_spring.security.TenantAccessService;
import com.eyram.dev.church_project_spring.utils.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** O(1) transactional change hints. Every read still checks current tenant access. */
@Service
public class DesktopRevisionService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ParoisseRepository parishes;
    private final TenantAccessService access;
    public DesktopRevisionService(NamedParameterJdbcTemplate jdbc,ParoisseRepository parishes,TenantAccessService access){this.jdbc=jdbc;this.parishes=parishes;this.access=access;}

    @Transactional(readOnly=true)
    public String revision(UUID parishId){return stamps(parishId,false);}
    @Transactional(readOnly=true)
    public String catalogue(UUID parishId){return stamps(parishId,true);}

    private String stamps(UUID parishId,boolean catalogue){
        var parish=parishes.findByPublicIdAndStatusDelFalse(parishId).orElseThrow(()->new ResourceNotFoundException("Paroisse introuvable"));
        access.checkParoisseAccess(parish);var user=access.getCurrentUser();
        String column=catalogue?"catalogue_revision":"revision";
        String stamp=jdbc.queryForObject("SELECT coalesce((SELECT "+column+" FROM desktop_sync_clock WHERE parish_id=:parish),0)::text || ':' || revision::text FROM desktop_global_clock WHERE id=1",Map.of("parish",parish.getId()),String.class);
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((parishId+":"+(catalogue?"catalogue":user.getPublicId())+":"+stamp).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
