package com.eyram.dev.church_project_spring.service.storage;

import com.eyram.dev.church_project_spring.config.StorageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoredFileServiceTest {

    @TempDir
    Path tempDir;

    private StoredFileService service;

    @BeforeEach
    void setUp() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setRoot(tempDir.toString());
        service = new StoredFileService(properties);
        service.init();
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deleteAfterCommitKeepsFileUntilCommitThenDeletesIt() {
        String path = storePdf();
        Path absolute = service.resolveAbsolute(path);

        TransactionSynchronizationManager.initSynchronization();
        service.deleteAfterCommit(path);

        assertTrue(Files.exists(absolute));
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        assertFalse(Files.exists(absolute));
    }

    @Test
    void deleteAfterCommitKeepsFileWhenTransactionRollsBack() {
        String path = storePdf();
        Path absolute = service.resolveAbsolute(path);

        TransactionSynchronizationManager.initSynchronization();
        service.deleteAfterCommit(path);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK
                )
        );

        assertTrue(Files.exists(absolute));
    }

    private String storePdf() {
        MockMultipartFile file = new MockMultipartFile(
                "mandat",
                "mandat.pdf",
                "application/pdf",
                "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII)
        );
        return service.storeInscriptionDocument(file, "mandat");
    }
}
