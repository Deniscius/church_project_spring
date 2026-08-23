package com.eyram.dev.church_project_spring.service.storage;

import com.eyram.dev.church_project_spring.config.StorageProperties;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Stockage local sécurisé : chemins relatifs sous {@code app.storage.root},
 * validation MIME réelle (magic bytes), optimisation logos, noms opaques.
 */
@Service
@RequiredArgsConstructor
public class StoredFileService {

    private static final Set<String> DOCUMENT_CONTENT_TYPES = Set.of(
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/jpg"
    );

    private static final Set<String> LOGO_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/jpg",
            "image/webp"
    );

    private final StorageProperties storageProperties;
    private Path root;

    @PostConstruct
    void init() throws IOException {
        root = Path.of(storageProperties.getRoot()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Files.createDirectories(root.resolve("inscriptions"));
        Files.createDirectories(root.resolve("logos"));
    }

    public String storeInscriptionDocument(MultipartFile file, String kind) {
        byte[] bytes = readBytes(file);
        validateDocument(file, bytes);
        String sniffed = sniffContentType(bytes);
        if (!DOCUMENT_CONTENT_TYPES.contains(sniffed) && !"image/jpg".equals(sniffed)) {
            throw new BusinessRuleException(
                    "Documents acceptés : PDF, JPEG ou PNG (scan du mandat / de la pièce d'identité)"
            );
        }
        String extension = extensionForContentType(sniffed);
        return storeBytes(bytes, "inscriptions/" + sanitizeKind(kind), extension);
    }

    public String storeParishLogo(MultipartFile file, UUID paroissePublicId) {
        byte[] bytes = readBytes(file);
        validateLogo(file, bytes);
        String sniffed = sniffContentType(bytes);
        if (!LOGO_CONTENT_TYPES.contains(sniffed) && !"image/jpg".equals(sniffed)) {
            throw new BusinessRuleException("Logo accepté : JPEG, PNG ou WebP");
        }
        ImageOptimizer.OptimizedImage optimized = ImageOptimizer.optimizeLogo(
                bytes,
                sniffed,
                storageProperties.getLogoMaxSidePx(),
                storageProperties.getLogoJpegQuality()
        );
        return storeBytes(
                optimized.bytes(),
                "logos/" + paroissePublicId,
                optimized.extension()
        );
    }

    /**
     * Programme la suppression après commit afin qu'un rollback SQL ne laisse
     * jamais la base pointer vers un fichier déjà supprimé.
     */
    public void deleteAfterCommit(String... relativePaths) {
        Runnable cleanup = () -> {
            if (relativePaths == null) {
                return;
            }
            for (String relativePath : relativePaths) {
                deleteQuietly(relativePath);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cleanup.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cleanup.run();
            }
        });
    }

    public void deleteQuietly(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        try {
            Path target = resolveSafe(relativePath);
            Files.deleteIfExists(target);
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    public Resource loadAsResource(String relativePath) {
        try {
            Path file = resolveSafe(relativePath);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessRuleException("Fichier introuvable");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BusinessRuleException("Fichier introuvable");
        }
    }

    public Path resolveAbsolute(String relativePath) {
        return resolveSafe(relativePath);
    }

    public String detectContentType(String relativePath) {
        try {
            Path file = resolveSafe(relativePath);
            byte[] header;
            try (var in = Files.newInputStream(file)) {
                header = in.readNBytes(16);
            }
            String sniffed = sniffContentType(header);
            if (StringUtils.hasText(sniffed)) {
                return sniffed.equals("image/jpg") ? MediaType.IMAGE_JPEG_VALUE : sniffed;
            }
            String probed = Files.probeContentType(file);
            if (StringUtils.hasText(probed)) {
                return probed;
            }
        } catch (IOException | BusinessRuleException ignored) {
            // fallback below
        }
        String lower = relativePath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF_VALUE;
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String storeBytes(byte[] bytes, String subdir, String extension) {
        String filename = UUID.randomUUID() + extension;
        try {
            Path dir = root.resolve(subdir).normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessRuleException("Chemin de stockage invalide");
            }
            Files.write(target, bytes);
            return root.relativize(target).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new BusinessRuleException("Impossible d'enregistrer le fichier");
        }
    }

    private Path resolveSafe(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new BusinessRuleException("Chemin de fichier manquant");
        }
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
            throw new BusinessRuleException("Fichier introuvable");
        }
        return resolved;
    }

    private void validateDocument(MultipartFile file, byte[] bytes) {
        validateCommon(file, bytes.length, storageProperties.getMaxFileBytes());
        String declared = normalizeContentType(file.getContentType());
        if (StringUtils.hasText(declared)
                && !declared.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                && !DOCUMENT_CONTENT_TYPES.contains(declared)) {
            throw new BusinessRuleException(
                    "Documents acceptés : PDF, JPEG ou PNG (scan du mandat / de la pièce d'identité)"
            );
        }
    }

    private void validateLogo(MultipartFile file, byte[] bytes) {
        validateCommon(file, bytes.length, storageProperties.getMaxLogoBytes());
        String declared = normalizeContentType(file.getContentType());
        if (StringUtils.hasText(declared)
                && !declared.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                && !LOGO_CONTENT_TYPES.contains(declared)) {
            // Certains OS envoient un MIME incorrect : on laisse le sniff décider.
            String sniffed = sniffContentType(bytes);
            if (!LOGO_CONTENT_TYPES.contains(sniffed) && !"image/jpg".equals(sniffed)) {
                throw new BusinessRuleException("Logo accepté : JPEG, PNG ou WebP");
            }
        }
    }

    private void validateCommon(MultipartFile file, long size, long maxBytes) {
        if (file == null || file.isEmpty() || size <= 0) {
            throw new BusinessRuleException("Fichier obligatoire");
        }
        if (size > maxBytes) {
            long mb = Math.max(1, maxBytes / (1024 * 1024));
            throw new BusinessRuleException("Fichier trop volumineux (max " + mb + " Mo)");
        }
        String name = file.getOriginalFilename();
        if (name != null && (name.contains("..") || name.contains("/") || name.contains("\\"))) {
            throw new BusinessRuleException("Nom de fichier invalide");
        }
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("Impossible de lire le fichier");
        }
    }

    /**
     * Détection réelle du type (PDF / JPEG / PNG / WebP) via signature binaire.
     */
    static String sniffContentType(byte[] header) {
        if (header == null || header.length < 4) {
            return "";
        }
        if (header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F') {
            return MediaType.APPLICATION_PDF_VALUE;
        }
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (header.length >= 8
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P') {
            return "image/webp";
        }
        return "";
    }

    private static String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "";
        }
        return contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
    }

    private static String sanitizeKind(String kind) {
        if (!StringUtils.hasText(kind)) {
            return "misc";
        }
        return kind.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase(Locale.ROOT);
    }

    private static String extensionForContentType(String contentType) {
        return switch (normalizeContentType(contentType)) {
            case MediaType.APPLICATION_PDF_VALUE -> ".pdf";
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
