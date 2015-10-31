package com.github.quadflask.smartcrop;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by flask on 2015. 10. 27..
 */
public class SmartCropTest {

    static String samplePath = "src/test/resources/sample";
    static String debugPath = "src/test/resources/debug";

    static String[] sampleImages = {"29386679.jpg",
            "32872321.jpg",
            "65131509.jpg",
            "65158073.jpg",
            "65309527.jpg",
            "65334383.jpg",
            "65356729.jpg",
            "65438769.jpg",
            "goodtimes.jpg",
            "guitarist.jpg",
            "img.jpg",
            "kitty.jpg",};

    static BufferedImage[] bufferedImages = new BufferedImage[sampleImages.length];
    static BufferedImage[] resultBufferedImages = new BufferedImage[sampleImages.length];

    @BeforeClass
    public static void setup() throws Exception {
        for (int i = 0; i < sampleImages.length; i++) {
            String name = sampleImages[i];
            bufferedImages[i] = ImageIO.read(new File(samplePath, name));
        }
    }

    @AfterClass
    public static void teardown() {
        for (int i = 0; i < sampleImages.length; i++) {
            final String name = sampleImages[i];
            final BufferedImage resultBufferedImage = resultBufferedImages[i];

            new Thread(() -> {
                try {
                    long b = System.currentTimeMillis();
                    ImageIO.write(resultBufferedImage, "png", new File(debugPath, name.replace("jpg", "png")));
                    System.out.println("sved... " + name + " / took " + (System.currentTimeMillis() - b) + "ms");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).run();
        }
    }

    @Test
    public void test() throws Exception {
        long b;
        for (int i = 0; i < sampleImages.length; i++) {
            String name = sampleImages[i];
            BufferedImage img = bufferedImages[i];

            b = System.currentTimeMillis();
            CropResult result = new SmartCrop().analyze(img);
            System.out.println("done: " + name + " / analyze took " + (System.currentTimeMillis() - b) + "ms");

            resultBufferedImages[i] = result.getBufferedImage();
        }
    }
}