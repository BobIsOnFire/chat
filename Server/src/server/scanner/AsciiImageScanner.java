package server.scanner;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;

public class AsciiImageScanner extends ImageScanner {
    private static final char[] ASCII_CHARS = {
            ' ', ' ', '.', '.', '"',
            ',', ';', ':', 'c', 'l',
            'o', 'd', 'x', 'k', 'O',
            '0', 'K', 'X', 'N', 'W', 'M'
    };


    public AsciiImageScanner(URL url, int consoleWidth) throws IOException {
        image = ImageIO.read(url);

        int width;
        int height;
        if (image.getWidth() > consoleWidth) {
            double ratio = image.getWidth() / (double) consoleWidth;
            width = (int) (image.getWidth() / ratio);
            height = (int) (image.getHeight() / ratio / 2);
        } else {
            width = image.getWidth();
            height = image.getHeight() / 2;
        }
        image = resize(width, height);
    }

    public String nextLine() {
        StringBuilder sb = new StringBuilder();
        while (hasNextCharInLine()) sb.append(nextCharInLine());
        sb.append('\n');
        i++;
        j = 0;
        return sb.toString();
    }

    public boolean hasNextLine() {
        return i < image.getHeight();
    }

    private char nextCharInLine() {
        int pixel = image.getRGB(j, i);

        int r = (pixel & 0x00FF0000) >> 16;
        int g = (pixel & 0x0000FF00) >> 8;
        int b = pixel & 0x000000FF;

        int grayScale = (r + g + b) / 3;
        int k = (int) Math.floor(grayScale / 256.0 * ASCII_CHARS.length);

        j++;
        return ASCII_CHARS[k];
    }

    private boolean hasNextCharInLine() {
        return j < image.getWidth();
    }
}
