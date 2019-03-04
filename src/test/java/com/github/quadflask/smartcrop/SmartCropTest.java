package com.github.quadflask.smartcrop;

import org.junit.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
		Arrays.stream(new File(samplePath).listFiles(pathname -> pathname.getName().endsWith(".jpg"))).
				forEach(file -> {
					try {
						System.out.println("Processing " + file.getName());
						BufferedImage input = ImageIO.read(file);

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

						Options options = new Options().prescale(true).scoreDownSample(1);
						SmartCrop smartCrop = SmartCrop.analyze(options.boost(faces), input);

						createCrop(input, smartCrop, options.aspect(1.0f), baseName + "_11", faces);
						createCrop(input, smartCrop, options.aspect(1.33f), baseName + "_1133", faces);
						createCrop(input, smartCrop, options.aspect(1.91f), baseName + "_1191", faces);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
	}

	private String baseName(String fileName) {
		int extenstionIndex = fileName.lastIndexOf('.');
		return fileName.substring(0, extenstionIndex);
	}

	private void createCrop(BufferedImage input, SmartCrop smartCrop, Options options, String filename, List<Boost> boost) throws Exception {
		CropResult cropResult = smartCrop.generateCrops(options);
		BufferedImage cropImage = createCrop(input, cropResult.topCrop);
		ImageIO.write(cropImage, "png", new File(resultPath, filename + ".png"));

		Graphics2D g = (Graphics2D) cropResult.debugImage.getGraphics();
		g.setColor(Color.WHITE);
		boost.forEach(b -> {
			g.drawRect(b.x, b.y, b.width, b.height);
		});
		g.dispose();
		ImageIO.write(cropResult.debugImage, "png", new File(debugPath, filename + ".png"));
	}

	private BufferedImage createCrop(BufferedImage input, Crop crop) {
		BufferedImage image = new BufferedImage(crop.width, crop.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(input, 0, 0, crop.width, crop.height, crop.x, crop.y, crop.x + crop.width, crop.y + crop.height, null);
		g.dispose();
		return image;
	}

}