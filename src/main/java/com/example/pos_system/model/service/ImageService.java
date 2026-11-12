package com.example.pos_system.model.service;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.common.ResourceNotFoundException;
import com.example.pos_system.model.entity.ProductImage;
import com.example.pos_system.model.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "png");
    private static final int MAX_SIDE = 640;
    private static final float JPEG_QUALITY = 0.82f;
    private static final String INVALID = "The file is not a valid JPEG or PNG image.";

    private final ProductImageRepository productImageRepository;

    @Value("${app.image.max-pixels:16000000}")
    private long maxPixels = 16_000_000L;

    // ---------- DB ----------

    @Transactional
    public ProductImage store(MultipartFile file) {
        return productImageRepository.save(new ProductImage("image/jpeg", process(file)));
    }

    @Transactional(readOnly = true)
    public ProductImage find(UUID id) {
        return productImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
    }

    @Transactional
    public void delete(UUID id) {
        productImageRepository.deleteById(id);
    }

    // ---------- Image Processing (No DB Interaction) ----------

    /** Inspect → Deconstruct → Compress → Re-render as a new JPEG */
    public byte[] process(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please select an image file.", HttpStatus.BAD_REQUEST);
        }
        byte[] raw;
        try {
            raw = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(INVALID, HttpStatus.BAD_REQUEST);
        }
        BufferedImage source = decode(raw);
        int[] size = fit(source.getWidth(), source.getHeight());
        return encodeJpeg(scale(source, size[0], size[1]));
    }

    private BufferedImage decode(byte[] raw) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(raw))){
            if (in == null) throw new BusinessException(INVALID, HttpStatus.BAD_REQUEST);

            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) throw new BusinessException(INVALID, HttpStatus.BAD_REQUEST);

            ImageReader reader = readers.next();
            try {
                if (!ALLOWED_FORMATS.contains(reader.getFormatName().toLowerCase())) {
                    throw new BusinessException(INVALID, HttpStatus.BAD_REQUEST);
                }
                reader.setInput(in, true, true);

                // Check size from the header first (not all pixels decoded yet)
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels >  maxPixels) {
                    throw new BusinessException(
                            "The image is too large (the number of pixels exceeds the limit)\n", HttpStatus.BAD_REQUEST);
                }
                return reader.read(0);

            }finally {
                reader.dispose();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new BusinessException(INVALID, HttpStatus.BAD_REQUEST);
        }
    }

    private int[] fit(int w, int h) {
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(w, h));   // Do not enlarge thumbnails
        return new int[] {
                Math.max(1, (int) Math.round(w * scale)),
                Math.max(1, (int) Math.round(h * scale))
        };
    }

    /** Compress in steps (halving the size each time) to achieve better quality than compressing all at once */
    private BufferedImage scale(BufferedImage src, int targetW, int targetH) {
        BufferedImage current = src;
        int w = src.getWidth();
        int h = src.getHeight();
        while (w / 2 >= targetW && h/ 2 >= targetH) {
            w /= 2;
            h /= 2;
            current = draw(current, w, h);
        }
        return draw(current, targetW, targetH); // At least once → Always RGB with a white background
    }

    private BufferedImage draw(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setColor(Color.WHITE);        // Transparent PNG → White background
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, w, h, null);

        } finally {
            g.dispose();
        }
        return out;
    }

    private byte[] encodeJpeg(BufferedImage img) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
            ios.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(INVALID, HttpStatus.MULTI_STATUS);
        } finally {
            writer.dispose();
        }
    }
}
