package com.example.pos_system;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.model.repository.ProductImageRepository;
import com.example.pos_system.model.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ImageServiceTest {

    private final ImageService service = new ImageService(mock(ProductImageRepository.class));

    private BufferedImage solid(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private byte[] encode(BufferedImage img, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    private MockMultipartFile file(String name, String type, byte[] bytes) {
        return new MockMultipartFile("file", name, type, bytes);
    }

    private BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    @Test
    void process_shrinksLargeImageToMax640AndOutputsJpeg() throws IOException {
        byte[] png = encode(solid(2000, 1000, Color.BLUE), "png");

        byte[] out = service.process(file("big.png", "image/png", png));

        assertThat(out[0]).isEqualTo((byte) 0xFF);   // JPEG magic bytes
        assertThat(out[1]).isEqualTo((byte) 0xD8);
        BufferedImage result = decode(out);
        assertThat(result.getWidth()).isEqualTo(640);
        assertThat(result.getHeight()).isEqualTo(320);   // រក្សាសមាមាត្រ
    }

    @Test
    void process_doesNotUpscaleSmallImages() throws IOException {
        byte[] png = encode(solid(200, 100, Color.RED), "png");

        BufferedImage result = decode(service.process(file("s.png", "image/png", png)));

        assertThat(result.getWidth()).isEqualTo(200);
        assertThat(result.getHeight()).isEqualTo(100);
    }

    @Test
    void process_transparentPngBecomesWhiteBackground() throws IOException {
        BufferedImage transparent = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

        BufferedImage result = decode(service.process(
                file("t.png", "image/png", encode(transparent, "png"))));

        Color c = new Color(result.getRGB(50, 50));
        assertThat(c.getRed()).isGreaterThan(240);
        assertThat(c.getGreen()).isGreaterThan(240);
        assertThat(c.getBlue()).isGreaterThan(240);
    }

    @Test
    void process_rejectsNonImageEvenIfNamedJpg() {
        MockMultipartFile fake = file(
                "fake.jpg",
                "image/jpeg",
                "hello, not an image".getBytes()
        );

        assertThatThrownBy(() -> service.process(fake))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not a valid JPEG or PNG image");
    }

    @Test
    void process_rejectsGif() throws IOException {
        byte[] gif = encode(solid(50, 50, Color.GREEN), "gif");

        assertThatThrownBy(() -> service.process(file("a.gif", "image/gif", gif)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void process_rejectsTooManyPixels() throws IOException {
        ReflectionTestUtils.setField(service, "maxPixels", 1000L);

        byte[] png = encode(
                solid(100, 100, Color.BLACK),
                "png"
        );

        assertThatThrownBy(() ->
                service.process(file("p.png", "image/png", png))
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("image is too large");
    }

    @Test
    void process_rejectsEmptyFile() {
        assertThatThrownBy(() -> service.process(file("e.png", "image/png", new byte[0])))
                .isInstanceOf(BusinessException.class);
    }
}
