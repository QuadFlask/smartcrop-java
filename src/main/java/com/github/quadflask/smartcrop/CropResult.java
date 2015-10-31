package com.github.quadflask.smartcrop;

import java.awt.image.BufferedImage;
import java.util.List;

public class CropResult {
    public Crop topCrop;
    public List<Crop> crops;
    public BufferedImage bufferedImage;

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
    }
}
