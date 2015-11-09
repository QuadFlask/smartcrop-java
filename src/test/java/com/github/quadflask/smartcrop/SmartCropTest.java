package com.github.quadflask.smartcrop;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

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
		bufferedImages.forEach((name, img) -> {
			long b = System.currentTimeMillis();
			Options options = new Options();
			options.setBufferedBitmapType(BufferedImage.TYPE_INT_RGB);
			CropResult result = new SmartCrop(options).analyze(img);
			System.out.println("done: " + name + " / analyze took " + (System.currentTimeMillis() - b) + "ms");

			resultBufferedImages.put(name, result.getBufferedImage());
		});
	}
}