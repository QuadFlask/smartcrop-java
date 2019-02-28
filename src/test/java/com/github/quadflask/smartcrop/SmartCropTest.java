package com.github.quadflask.smartcrop;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by flask on 2015. 10. 27..
 */
public class SmartCropTest {

	static String samplePath = "src/test/resources/sample";
	static String debugPath = "src/test/resources/debug";
	static String resultPath = "src/test/resources/result";

	static Map<String, BufferedImage> bufferedImages = new ConcurrentHashMap<>();
	static Map<String, CropResult> cropResults = new ConcurrentHashMap<>();

	@BeforeClass
	public static void setup() throws Exception {
		Arrays.stream(new File(samplePath)
				.listFiles(pathname -> pathname.getName().endsWith(".jpg")))
				.forEach(file -> {
					try {
						bufferedImages.put(file.getName(), ImageIO.read(file));
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
	}

	@AfterClass
	public static void teardown() {
		cropResults.forEach((name, cropResult) -> {
			try {
				long b = System.currentTimeMillis();
				String newName = name.replace("jpg", "png");
				ImageIO.write(cropResult.debugImage, "png", new File(debugPath, newName));

				BufferedImage resultImage = createCrop(bufferedImages.get(name), cropResult.topCrop);
				ImageIO.write(resultImage, "png", new File(resultPath, newName));
				System.out.println("saved... " + newName + " / took " + (System.currentTimeMillis() - b) + "ms");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	@Test
	public void test() throws Exception {
		final AtomicLong pixels = new AtomicLong();

		long total = System.currentTimeMillis();

		bufferedImages.forEach((name, img) -> {
			long b = System.currentTimeMillis();

			CropResult result = SmartCrop.analyze(new Options(), img);

			System.out.println("done: " + name + " / analyze took " + (System.currentTimeMillis() - b) + "ms");
			pixels.addAndGet(img.getWidth() * img.getHeight());
			cropResults.put(name, result);
		});

		System.out.println(((pixels.get() / ((System.currentTimeMillis() - total) / 1000f)) / 1000f / 1000f) + " MPixels/s");
	}

	private static BufferedImage createCrop(BufferedImage input, Crop crop) {
		BufferedImage image = new BufferedImage(crop.width, crop.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(input, 0, 0, crop.width, crop.height, crop.x, crop.y, crop.x + crop.width, crop.y + crop.height, null);
		g.dispose();
		return image;
	}
}