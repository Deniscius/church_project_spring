package com.eyram.dev.church_project_spring.service.storage;

import com.eyram.dev.church_project_spring.utils.exception.BusinessRuleException;
import org.springframework.http.MediaType;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * Redimensionne et recompresse les logos (JPEG/PNG) pour limiter poids disque / PDF.
 * WebP : validé mais stocké tel quel (pas de décodeur JDK standard).
 */
final class ImageOptimizer {

    record OptimizedImage(byte[] bytes, String contentType, String extension) {
    }

    private ImageOptimizer() {
    }

    static OptimizedImage optimizeLogo(byte[] raw, String contentType, int maxSidePx, float jpegQuality) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if ("image/webp".equals(ct)) {
            return new OptimizedImage(raw, "image/webp", ".webp");
        }

        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(raw));
        } catch (IOException e) {
            throw new BusinessRuleException("Logo illisible — utilisez un JPEG ou PNG valide");
        }
        if (source == null) {
            throw new BusinessRuleException("Logo illisible — utilisez un JPEG ou PNG valide");
        }

        BufferedImage scaled = scaleDown(source, maxSidePx);
        boolean hasAlpha = scaled.getColorModel().hasAlpha();

        try {
            if (hasAlpha || MediaType.IMAGE_PNG_VALUE.equals(ct)) {
                byte[] png = writePng(scaled);
                // Si le PNG reste plus lourd que l'original et qu'il n'y a pas d'alpha utile, JPEG.
                if (!hasAlpha && png.length >= raw.length && raw.length > 0) {
                    byte[] jpeg = writeJpeg(toRgb(scaled), jpegQuality);
                    if (jpeg.length < png.length) {
                        return new OptimizedImage(jpeg, MediaType.IMAGE_JPEG_VALUE, ".jpg");
                    }
                }
                return new OptimizedImage(png, MediaType.IMAGE_PNG_VALUE, ".png");
            }
            byte[] jpeg = writeJpeg(toRgb(scaled), jpegQuality);
            // Ne jamais grossir le fichier : garder l'original si déjà plus léger.
            if (jpeg.length >= raw.length && isAlreadySmall(raw, maxSidePx, source)) {
                String ext = MediaType.IMAGE_PNG_VALUE.equals(ct) ? ".png" : ".jpg";
                String outCt = MediaType.IMAGE_PNG_VALUE.equals(ct)
                        ? MediaType.IMAGE_PNG_VALUE
                        : MediaType.IMAGE_JPEG_VALUE;
                return new OptimizedImage(raw, outCt, ext);
            }
            return new OptimizedImage(jpeg, MediaType.IMAGE_JPEG_VALUE, ".jpg");
        } catch (IOException e) {
            throw new BusinessRuleException("Impossible d'optimiser le logo");
        }
    }

    private static boolean isAlreadySmall(byte[] raw, int maxSidePx, BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        return Math.max(w, h) <= maxSidePx && raw.length < 200_000;
    }

    private static BufferedImage scaleDown(BufferedImage source, int maxSidePx) {
        int w = source.getWidth();
        int h = source.getHeight();
        int max = Math.max(w, h);
        if (max <= maxSidePx || maxSidePx <= 0) {
            return source;
        }
        double ratio = (double) maxSidePx / max;
        int nw = Math.max(1, (int) Math.round(w * ratio));
        int nh = Math.max(1, (int) Math.round(h * ratio));
        int type = source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage target = new BufferedImage(nw, nh, type);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.drawImage(source, 0, 0, java.awt.Color.WHITE, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static byte[] writePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", bos)) {
            throw new IOException("Écriture PNG impossible");
        }
        return bos.toByteArray();
    }

    private static byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        float q = Math.min(1f, Math.max(0.5f, quality));
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("Aucun encodeur JPEG");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(q);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return bos.toByteArray();
    }
}
