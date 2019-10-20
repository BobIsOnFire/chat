package server;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class ASCIIImageScanner {
    private static final char[] ASCII_CHARS = {
            ' ', ' ', '.', '.', '"',
            ',', ';', ':', 'c', 'l',
            'o', 'd', 'x', 'k', 'O',
            '0', 'K', 'X', 'N', 'W', 'M'
    };

    private BufferedImage image;
    private int i = 0;
    private int j = 0;

    ASCIIImageScanner(URL url, int consoleWidth) throws IOException {
        image = ImageIO.read(url);

        if (image.getWidth() > consoleWidth) {
            double ratio = image.getWidth() / (double) consoleWidth;
            int width = (int) (image.getWidth() / ratio);
            int height = (int) (image.getHeight() / ratio / 2);
            image = resize(width, height);
        }
    }

    String nextLine() {
        StringBuilder sb = new StringBuilder();
        while (hasNextCharInLine()) sb.append(nextCharInLine());
        sb.append('\n');
        i++;
        j = 0;
        return sb.toString();
    }

    boolean hasNextLine() {
        return i < image.getHeight();
    }

    char nextCharInLine() {
        int pixel = image.getRGB(j, i);

        int r = (pixel & 0x00FF0000) >> 16;
        int g = (pixel & 0x0000FF00) >> 8;
        int b = pixel & 0x000000FF;

        int grayScale = (r + g + b) / 3;
        int k = (int) Math.floor(grayScale / 256.0 * ASCII_CHARS.length);

        j++;
        return ASCII_CHARS[k];
    }

    boolean hasNextCharInLine() {
        return j < image.getWidth();
    }

    private BufferedImage resize(int width, int height) {
        final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setComposite(AlphaComposite.Src);

        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        graphics2D.drawImage(image, 0, 0, width, height, null);
        graphics2D.dispose();
        return bufferedImage;
    }
}
