package server.scanner;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;

public class AnsiImageScanner extends ImageScanner {
    private int previousColor = -1;

    public AnsiImageScanner(URL url, int consoleWidth) throws IOException {
        image = ImageIO.read(url);

        if (image.getWidth() > consoleWidth / 2) {
            double ratio = image.getWidth() / (double) consoleWidth * 2;
            int width = (int) (image.getWidth() / ratio);
            int height = (int) (image.getHeight() / ratio);
            image = resize(width, height);
        }

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

    private String nextSymbolInLine() {
        int pixel = image.getRGB(j, i);

        int B = pixel & 0x000000FF;
        int G = (pixel & 0x0000FF00) >> 8;
        int R = (pixel & 0x00FF0000) >> 16;

        int color;
        if (R == G && R == B) color = greyScaleColor(R);
        else if (R % 255 == 0 && G % 255 == 0 && B % 255 == 0) color = brightScaleColor(R, G, B);
        else if (R % 128 == 0 && R % 128 == 0 && R % 128 == 0) color = paleScaleColor(R, G, B);
        else color = cubeScaleColor(R, G, B);

        String symbol;
        if (previousColor == color) symbol = "  ";
        else symbol = "\u001B[48;5;" + color + "m  ";

        previousColor = color;
        j++;
        return symbol;
    }

    private boolean hasNextSymbolInLine() {
        return j < image.getWidth();
    }

    private int greyScaleColor(int color) {
        if (color == 0) return 0;
        if (color == 128) return 8;
        if (color == 192) return 7;
        if (color == 255) return 15;

        return 232 + (int) Math.floor(color / 256.0 * 24);
    }

    private int brightScaleColor(int r, int g, int b) {
        return 8 + r / 255 + g * 2 / 255 + b * 4 / 255;
    }

    private int paleScaleColor(int r, int g, int b) {
        return r / 128 + g * 2 / 128 + b * 4 / 128;
    }

    private int cubeScaleColor(int r, int g, int b) {
        int cubeR = (int) Math.floor(r / 256.0 * 6);
        int cubeG = (int) Math.floor(g / 256.0 * 6);
        int cubeB = (int) Math.floor(b / 256.0 * 6);

        return 16 + 36 * cubeR + 6 * cubeG + cubeB;
    }
}
