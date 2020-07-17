package server.scanner;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;

public class AnsiImageScanner extends ImageScanner {
    private int previousColor = -1;

    public AnsiImageScanner(URL url, int consoleWidth) throws IOException {
        image = ImageIO.read(url);
        if (image == null) throw new IOException();

        if (image.getWidth() > consoleWidth / 2) {
            double ratio = image.getWidth() / (double) consoleWidth * 2;
            int width = (int) (image.getWidth() / ratio);
            int height = (int) (image.getHeight() / ratio);
            image = resize(width, height);
        }

        System.out.printf("Width: %d, Height: %d\n", image.getWidth(), image.getHeight());
    }

    public String nextLine() {
        StringBuilder sb = new StringBuilder();
        while (hasNextSymbolInLine()) sb.append(nextSymbolInLine());
        sb.append("\u001B[49m\n");
        i++;
        j = 0;
        previousColor = -1;
        return sb.toString();
    }

    public boolean hasNextLine() {
        return i < image.getHeight();
    }

    public String charAt(int x, int y) {
        return charAt(x, y, false);
    }

    private String charAt(int x, int y, boolean preserveColor) {
        int pixel = image.getRGB(x, y);

        int A = pixel >> 24 & 0xFF;

        int B = (pixel & 0xFF) * A / 0xFF;
        int G = (pixel >> 8 & 0xFF) * A / 0xFF;
        int R = (pixel >> 16 & 0xFF) * A / 0xFF;

        int color = determineColor(R, G, B);

        String symbol;
        if (preserveColor) {
            if (previousColor == color) symbol = "  ";
            else symbol = "\u001B[48;5;" + color + "m  ";
            previousColor = color;
        } else symbol = "\u001B[48;5;" + color + "m  \u001B[49m";

        j++;
        if (!preserveColor) return String.format("%X\t%d\t%s", image.getRGB(x, y), color, symbol);
        return symbol;
    }

    private int determineColor(int R, int G, int B) {
        if (R % 187 == 0 && G % 187 == 0 && B % 187 == 0)
            return R / 187 + G * 2 / 187 + B * 4 / 187; // pale scale (0-7)

        if ((R - 65) % 210 == 0 && (G - 65) % 210 == 0 && (B - 65) % 210 == 0)
            return 8 + R / 255 + G * 2 / 255 + B * 4 / 255; // bright scale (8-15)

        int r = (R + 4) / 5 * 5 - 2;
        int g = (G + 4) / 5 * 5 - 2;
        int b = (B + 4) / 5 * 5 - 2;

        if (r == g && r == b) { // grey scale (232-255)
            if (r < 8) return 0;
            if (r > 243) return 15;

            return 232 + (r - 8) / 10;
        }

        int cubeR = (r <= 48) ? 0 : Math.max(0, (r - 75) / 40 + 1);
        int cubeG = (g <= 48) ? 0 : Math.max(0, (g - 75) / 40 + 1);
        int cubeB = (b <= 48) ? 0 : Math.max(0, (b - 75) / 40 + 1);

        return 16 + 36 * cubeR + 6 * cubeG + cubeB;
    }

    private String nextSymbolInLine() {
        return charAt(j, i, true);
    }

    private boolean hasNextSymbolInLine() {
        return j < image.getWidth();
    }

}
