package com.example.pos_system;

import com.example.pos_system.model.entity.ProductImage;
import com.example.pos_system.model.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ImagePersistenceIntegrationTest extends IntegrationTestBase {

    @Autowired
    ImageService imageService;

    @Test
    void imageBytesRoundTripThroughPostgresBytea() throws IOException {
        BufferedImage img = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, 1200, 800);
        g.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(img, "png", png);

        ProductImage saved = imageService.store(
                new MockMultipartFile("file", "a.png", "image/png", png.toByteArray()));
        ProductImage loaded = imageService.find(saved.getId());

        assertThat(loaded.getContentType()).isEqualTo("image/jpeg");
        assertThat(loaded.getData()).isEqualTo(saved.getData()).hasSizeGreaterThan(100);
        assertThat(loaded.getData()[0]).isEqualTo((byte) 0xFF);     // JPEG magic bytes
        assertThat(loaded.getSizeBytes()).isEqualTo(loaded.getData().length);

        imageService.delete(saved.getId());
    }
}