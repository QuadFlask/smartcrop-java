package com.github.quadflask.smartcrop;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
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

	static Map<String, BufferedImage> bufferedImages = new ConcurrentHashMap<>();
	static Map<String, BufferedImage> resultBufferedImages = new ConcurrentHashMap<>();

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
		resultBufferedImages.forEach((name, resultBufferedImage) -> {
			new Thread(() -> {
				try {
					long b = System.currentTimeMillis();
					String newName = name; // name.replace("jpg", "png");
					ImageIO.write(resultBufferedImage, "jpg", new File(debugPath, newName));
					System.out.println("saved... " + newName + " / took " + (System.currentTimeMillis() - b) + "ms");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}).run();
		});
	}

	@Test
	public void test() throws Exception {
		Options options = new Options().bufferedBitmapType(BufferedImage.TYPE_INT_RGB);
		final AtomicLong pixels = new AtomicLong();

		long total = System.currentTimeMillis();

		bufferedImages.forEach((name, img) -> {
			long b = System.currentTimeMillis();
			CropResult result = new SmartCrop(options).analyze(img);
			System.out.println("done: " + name + " / analyze took " + (System.currentTimeMillis() - b) + "ms");
			pixels.addAndGet(img.getWidth() * img.getHeight());

			resultBufferedImages.put(name, result.getBufferedImage());
		});

		System.out.println(((pixels.get() / ((System.currentTimeMillis() - total) / 1000)) / 1000 / 1000f) + " MPixels/s");
	}
}