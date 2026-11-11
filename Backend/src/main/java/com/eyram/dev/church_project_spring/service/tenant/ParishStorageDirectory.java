package com.eyram.dev.church_project_spring.service.tenant;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.eyram.dev.church_project_spring.utils.exception.ParishStorageUnavailableException;

/** Storage placement is independent of authorization. No physical shard routing is enabled yet. */
@Service
public class ParishStorageDirectory {
    private final JdbcTemplate jdbc;

    public ParishStorageDirectory(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** The caller must authorize the parish first. Keep placement locked until the operation commits. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireSharedStorage(UUID parishId) {
        if (parishId == null) throw unavailable();
        var placements = jdbc.queryForList(
                "SELECT shard_key FROM parish_storage_placement WHERE parish_id=? FOR SHARE",
                String.class, parishId);
        if (placements.size() != 1 || !"primary".equals(placements.get(0))) throw unavailable();
    }

    private ParishStorageUnavailableException unavailable() {
        // 503 keeps desktop operations pending; a deployment problem is not a business rejection.
        return new ParishStorageUnavailableException();
    }
}
