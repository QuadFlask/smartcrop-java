package com.github.quadflask.smartcrop;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencv.core.Rect;

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
	static String resultPath = "src/test/resources/result";

	static Map<String, BufferedImage> bufferedImages = new ConcurrentHashMap<>();
	static Map<String, CropResult> cropResults = new ConcurrentHashMap<>();

	@BeforeClass
	public static void setup() throws Exception {
		Arrays.stream(new File(samplePath)
				.listFiles(pathname -> pathname.getName().endsWith("g")))
				.forEach(file -> {
					try {
						bufferedImages.put(file.getName(), ImageIO.read(file));
					} catch (IOException e) {
						e.printStackTrace();
					}
				});


		OpencvDetect.getInstance().SetFrontalFacePath("/usr/local/Cellar/opencv/4.0.1/share/opencv4/haarcascades/haarcascade_frontalface_alt.xml");
	}

	@AfterClass
	public static void teardown() {
		cropResults.forEach((name, cropResult) -> {
			new Thread(() -> {
				try {
					long b = System.currentTimeMillis();
					String newName = name; // name.replace("jpg", "png");
					ImageIO.write(cropResult.debugImage, "jpg", new File(debugPath, newName));
					ImageIO.write(cropResult.resultImage, "jpg", new File(resultPath, newName));
					System.out.println( "score:"+cropResult.topCrop.score.total+ " saved... " + newName + " / took " + (System.currentTimeMillis() - b) + "ms");
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

			options.setCropWidth(200);
			options.setCropHeight(250);
			options.setDebug(true);
			options.setBoost(OpencvDetect.getInstance().detectFace(img));
			CropResult result = SmartCrop.analyze(options, img);

//			System.out.println("done: " + name + " / analyze took " + (System.currentTimeMillis() - b) + "ms");
			pixels.addAndGet(img.getWidth() * img.getHeight());
			cropResults.put(name, result);
		});

		System.out.println(((pixels.get() / ((System.currentTimeMillis() - total) / 1000)) / 1000 / 1000f) + " MPixels/s");
	}
}