package com.github.quadflask.smartcrop;

import org.junit.Test;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;

/**
 * Created by flask on 2015. 10. 27..
 */
public class SmartCropTest {

	static String samplePath = "src/test/resources/sample";
	static String debugPath = "src/test/resources/debug";
	static String resultPath = "src/test/resources/result";

	private FaceDetector faceDetector = new DnnFaceDetector();

	@Test
	public void test() throws Exception {
		AtomicLong pixels = new AtomicLong();
		AtomicLong total = new AtomicLong();

		Arrays.stream(new File(samplePath).listFiles(pathname -> pathname.getName().endsWith(".jpg"))).
				forEach(file -> {
					try {
						System.out.println("Processing " + file.getName());
						BufferedImage input = ImageIO.read(file);
						pixels.addAndGet(input.getWidth() * input.getHeight());

						List<Boost> faces = new ArrayList<>();
						faceDetector.detect(input, (x, y, width, height, confidence) -> {
							Boost boost = new Boost();
							boost.x = x;
							boost.y = y;
							boost.width = width;
							boost.height = height;
							boost.weight = 1.0f;
							faces.add(boost);
						});
						System.out.println("Detected " + faces.size() + " faces");
						String baseName = baseName(file.getName());

						long startTime = System.currentTimeMillis();
						Options options = new Options().
								prescale(true).
								scoreDownSample(8).
								boost(faces).
								aspect(1.0f).
								ruleOfThirds(true).
								minScale(1.0f);
						SmartCrop smartCrop = SmartCrop.analyze(options, input);
						CropResult cropResult = smartCrop.generateCrops(options);
						total.addAndGet(System.currentTimeMillis() - startTime);

						BufferedImage cropImage = createCropImage(input, cropResult.topCrop);
						ImageIO.write(cropImage, "jpg", new File(resultPath, baseName + ".jpg"));

						BufferedImage debugImage = smartCrop.createDebugOutput(cropResult.topCrop, faces);
						ImageIO.write(debugImage, "jpg", new File(debugPath, baseName + ".jpg"));
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

		System.out.println((pixels.get() / (total.get() * 1000f)) + " MPixels/s");
	}

	private String baseName(String fileName) {
		int extenstionIndex = fileName.lastIndexOf('.');
		return fileName.substring(0, extenstionIndex);
	}

	private BufferedImage createCropImage(BufferedImage input, Crop crop) {
		BufferedImage image = new BufferedImage(crop.width, crop.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(input, 0, 0, crop.width, crop.height, crop.x, crop.y, crop.x + crop.width, crop.y + crop.height, null);
		g.dispose();
		return image;
	}

}