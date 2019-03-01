package com.github.quadflask.smartcrop;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_dnn;
import org.bytedeco.javacpp.opencv_java;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.junit.BeforeClass;
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

import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_dnn.blobFromImage;
import static org.bytedeco.javacpp.opencv_dnn.readNetFromCaffe;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 * Created by flask on 2015. 10. 27..
 */
public class SmartCropTest {

	static String samplePath = "src/test/resources/sample";
	static String debugPath = "src/test/resources/debug";
	static String resultPath = "src/test/resources/result";

	private static opencv_dnn.Net faceNet;

	@BeforeClass
	public static void setUp() {
		Loader.load(opencv_java.class);

		faceNet = readNetFromCaffe("src/test/resources/deploy.prototxt.txt", "src/test/resources/res10_300x300_ssd_iter_140000.caffemodel");
	}

	@Test
	public void test() throws Exception {
		Arrays.stream(new File(samplePath).listFiles(pathname -> pathname.getName().endsWith(".jpg"))).
				forEach(file -> {
					try {
						System.out.println("Processing " + file.getName());
						BufferedImage input = ImageIO.read(file);
						List<Boost> faces = detectFaces(input);
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

	private List<Boost> detectFaces(BufferedImage input) {
		Mat frame = Java2DFrameUtils.toMat(input);

		resize(frame, frame, new Size(300, 300));
		Mat blob = blobFromImage(frame, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0, 0.0), false, false, CV_32F);

		faceNet.setInput(blob);
		Mat output = faceNet.forward();

		Mat ne = new Mat(new Size(output.size(3), output.size(2)), CV_32F, output.ptr(0, 0));
		FloatIndexer srcIndexer = ne.createIndexer();

		List<Boost> faces = new ArrayList<>();
		for (int i = 0; i < output.size(3); i++) {
			float confidence = srcIndexer.get(i, 2);

			if (confidence > .6f) {
				float f1 = srcIndexer.get(i, 3);
				float f2 = srcIndexer.get(i, 4);
				float f3 = srcIndexer.get(i, 5);
				float f4 = srcIndexer.get(i, 6);

				Boost face = new Boost();
				face.x = (int) (f1 * input.getWidth());
				face.y = (int) (f2 * input.getHeight());
				face.width = (int) ((f3 - f1) * input.getWidth());
				face.height = (int) ((f4 - f2) * input.getHeight());
				face.weight = 1.0f;
				faces.add(face);
			}
		}

		srcIndexer.release();
		ne.release();
		output.release();
		blob.release();
		frame.release();

		return faces;
	}

}