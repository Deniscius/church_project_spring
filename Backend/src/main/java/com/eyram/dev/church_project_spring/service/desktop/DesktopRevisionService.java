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

/** A tenant-scoped change hint, not a data feed. Every read still passes normal API authorization. */
@Service
public class DesktopRevisionService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ParoisseRepository parishes;
    private final TenantAccessService access;
    private static String stamp(String source,String from,String where){
        return "SELECT '"+source+"' AS source, count(*)::text || ':' || coalesce(max(coalesce(t.updated_at,t.created_at))::text,'') AS value FROM "+from+" WHERE "+where;
    }
    private static final String SOURCES=String.join(" UNION ALL ",
        stamp("demands","demande t","t.paroisse_id=:parish"),
        stamp("dates","demande_date t JOIN demande d ON d.id=t.demande_id","d.paroisse_id=:parish"),
        stamp("schedules","horaire t","t.paroisse_id=:parish"),
        stamp("types","type_demande t","t.paroisse_id=:parish"),
        stamp("prices","forfait_tarif t JOIN type_demande d ON d.id=t.type_demande_id","d.paroisse_id=:parish"),
        stamp("parish","paroisse t","t.id=:parish"),
        stamp("access","paroisse_access t","t.paroisse_id=:parish"),
        stamp("members","users t JOIN paroisse_access a ON a.user_id=t.id","a.paroisse_id=:parish"),
        stamp("self","users t","t.id=:user"),
        stamp("choirs","chorale t","t.paroisse_id=:parish"),
        stamp("invoices","facture t JOIN demande d ON d.id=t.demande_id","d.paroisse_id=:parish"),
        stamp("payments","details_paiement t JOIN facture f ON f.id=t.facture_id JOIN demande d ON d.id=f.demande_id","d.paroisse_id=:parish"),
        stamp("account","compte_paroisse t","t.paroisse_id=:parish"),
        stamp("transfers","demande_reversement t","t.paroisse_id=:parish"),
        stamp("payment-types","type_paiement t","true"),
        "SELECT 'type-days',coalesce(string_agg(j.type_demande_id::text || ':' || j.jour_semaine,',' ORDER BY j.type_demande_id,j.jour_semaine),'') FROM type_demande_jour_autorise j JOIN type_demande d ON d.id=j.type_demande_id WHERE d.paroisse_id=:parish",
        "SELECT 'price-days',coalesce(string_agg(j.forfait_tarif_id::text || ':' || j.jour_semaine,',' ORDER BY j.forfait_tarif_id,j.jour_semaine),'') FROM forfait_tarif_jour_autorise j JOIN forfait_tarif f ON f.id=j.forfait_tarif_id JOIN type_demande d ON d.id=f.type_demande_id WHERE d.paroisse_id=:parish"
    );
    public DesktopRevisionService(NamedParameterJdbcTemplate jdbc,ParoisseRepository parishes,TenantAccessService access){this.jdbc=jdbc;this.parishes=parishes;this.access=access;}
    @Transactional(readOnly=true)
    public String revision(UUID parishId){
        var parish=parishes.findByPublicIdAndStatusDelFalse(parishId).orElseThrow(()->new ResourceNotFoundException("Paroisse introuvable"));
        access.checkParoisseAccess(parish);var user=access.getCurrentUser();
        String stamps=jdbc.queryForObject("SELECT string_agg(source || '=' || value,';' ORDER BY source) FROM ("+SOURCES+") revisions",Map.of("parish",parish.getId(),"user",user.getId()),String.class);
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((parishId+":"+user.getPublicId()+":"+stamps).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
