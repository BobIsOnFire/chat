package server.scanner;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class ImageScanner {
    public abstract String nextLine();
    public abstract boolean hasNextLine();

    BufferedImage image;
    int i = 0;
    int j = 0;

    BufferedImage resize(int width, int height) {
        final BufferedImage bufferedImage = new BufferedImage(width, height, image.getType());
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
