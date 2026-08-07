package com.eyram.dev.church_project_spring.service.storage;

import com.eyram.dev.church_project_spring.config.StorageProperties;
import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Stockage local sécurisé : chemins relatifs sous {@code app.storage.root},
 * validation MIME / taille, noms de fichiers opaques.
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
        validateDocument(file);
        return store(file, "inscriptions/" + sanitizeKind(kind));
    }

    public String storeParishLogo(MultipartFile file, UUID paroissePublicId) {
        validateLogo(file);
        return store(file, "logos/" + paroissePublicId);
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
            String probed = Files.probeContentType(resolveSafe(relativePath));
            if (StringUtils.hasText(probed)) {
                return probed;
            }
        } catch (IOException ignored) {
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

    private String store(MultipartFile file, String subdir) {
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()
        );
        String extension = extensionOf(original, file.getContentType());
        String filename = UUID.randomUUID() + extension;
        try {
            Path dir = root.resolve(subdir).normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessRuleException("Chemin de stockage invalide");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
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

    private void validateDocument(MultipartFile file) {
        validateCommon(file);
        String contentType = normalizeContentType(file.getContentType());
        if (!DOCUMENT_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleException(
                    "Documents acceptés : PDF, JPEG ou PNG (scan du mandat / de la pièce d'identité)"
            );
        }
    }

    private void validateLogo(MultipartFile file) {
        validateCommon(file);
        String contentType = resolveLogoContentType(file);
        if (!LOGO_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleException("Logo accepté : JPEG, PNG ou WebP");
        }
    }

    /**
     * Certains navigateurs / OS envoient {@code application/octet-stream} ou aucun MIME.
     * On s'appuie alors sur l'extension, puis sur la signature binaire du fichier.
     */
    private String resolveLogoContentType(MultipartFile file) {
        String contentType = normalizeContentType(file.getContentType());
        if (LOGO_CONTENT_TYPES.contains(contentType)) {
            return contentType;
        }
        if (StringUtils.hasText(contentType)
                && !contentType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            return contentType;
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = original.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }

        return sniffImageContentType(file);
    }

    private static String sniffImageContentType(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(12);
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
        } catch (IOException ignored) {
            // fall through
        }
        return "";
    }

    private void validateCommon(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Fichier obligatoire");
        }
        if (file.getSize() > storageProperties.getMaxFileBytes()) {
            long mb = storageProperties.getMaxFileBytes() / (1024 * 1024);
            throw new BusinessRuleException("Fichier trop volumineux (max " + mb + " Mo)");
        }
        String name = file.getOriginalFilename();
        if (name != null && (name.contains("..") || name.contains("/") || name.contains("\\"))) {
            throw new BusinessRuleException("Nom de fichier invalide");
        }
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

    private static String extensionOf(String filename, String contentType) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            String ext = filename.substring(dot).toLowerCase(Locale.ROOT);
            if (ext.matches("\\.(pdf|jpe?g|png|webp)")) {
                return ext.equals(".jpeg") ? ".jpg" : ext;
            }
        }
        String ct = normalizeContentType(contentType);
        return switch (ct) {
            case MediaType.APPLICATION_PDF_VALUE -> ".pdf";
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
