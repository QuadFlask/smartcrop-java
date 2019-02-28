package com.github.quadflask.smartcrop;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

/**
 * Created by flask on 2015. 10. 27..
 */
public class SmartCropTest {

	static String samplePath = "src/test/resources/sample";
	static String debugPath = "src/test/resources/debug";
	static String resultPath = "src/test/resources/result";

	@Test
	public void test() throws Exception {
		Arrays.stream(new File(samplePath).listFiles(pathname -> pathname.getName().endsWith(".jpg"))).
				forEach(file -> {
					try {
						BufferedImage input = ImageIO.read(file);
						String baseName = baseName(file.getName());

						Options options = new Options();
						SmartCrop smartCrop = SmartCrop.analyze(options, input);

						createCrop(input, smartCrop, options.aspect(1.0f), baseName + "_11");
						createCrop(input, smartCrop, options.aspect(1.33f), baseName + "_1133");
						createCrop(input, smartCrop, options.aspect(1.91f), baseName + "_1191");
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
	}

	private String baseName(String fileName) {
		int extenstionIndex = fileName.lastIndexOf('.');
		return fileName.substring(0, extenstionIndex);
	}

	private void createCrop(BufferedImage input, SmartCrop smartCrop, Options options, String filename) throws Exception {
		CropResult cropResult = smartCrop.generateCrops(options);
		BufferedImage cropImage = createCrop(input, cropResult.topCrop);
		ImageIO.write(cropImage, "png", new File(resultPath, filename + ".png"));
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