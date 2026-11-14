package com.missanye.desktop.service;

import com.missanye.desktop.api.ApiClient;
import java.util.function.LongSupplier;

/** Conditional revision checks; periodically reconcile fully, including older backend versions. */
public final class DataRevision {
    public record Check(String revision,boolean changed){}
    private String known,parishContext;
    private ApiClient clientContext;
    private long acknowledgedAt,legacyRetryAt;
    private final LongSupplier clock;
    public DataRevision(){this(System::currentTimeMillis);}
    public DataRevision(LongSupplier clock){this.clock=clock;}
    public Check check(ApiClient api,String parish)throws Exception{
        if(api!=clientContext||!parish.equals(parishContext)){reset();clientContext=api;parishContext=parish;}
        long now=clock.getAsLong();boolean full=acknowledgedAt==0||now-acknowledgedAt>=60000;
        if(legacyRetryAt>now)return new Check(null,full);
        try{String revision=api.revision(parish,known);return new Check(revision,full||!revision.equals(known));}
        catch(ApiClient.ApiException old){if(old.status()!=404&&old.status()!=405)throw old;legacyRetryAt=now+300000;return new Check(null,full);}
    }
    public void acknowledge(Check check){known=check.revision();acknowledgedAt=clock.getAsLong();}
    public void reset(){known=null;acknowledgedAt=0;legacyRetryAt=0;}
}
