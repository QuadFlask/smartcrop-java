package com.github.quadflask.smartcrop;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by flask on 2015. 10. 30..
 */
public class SmartCrop {
	private Options options;
	private int[] cd;

	public SmartCrop() {
		this(Options.DEFAULT);
	}

	public SmartCrop(Options options) {
		this.options = options;
	}

	public static CropResult analyze(Options options, BufferedImage input) {
		return new SmartCrop(options).analyze(input);
	}

	public CropResult analyze(BufferedImage input) {
		if (options.getAspect() != 0.0f) {
			options.width(options.getAspect());
			options.height(1.0f);
		}

		float scale = 1.0f;
		float prescale = 1.0f;

		// calculate desired crop dimensions based on the image size
		if (options.getWidth() != 0.0f && options.getHeight() != 0.0f) {
			scale = Math.min(input.getWidth() / options.getWidth(), input.getHeight() / options.getHeight());
			options.cropWidth((int) Math.floor(options.getWidth() * scale));
			options.cropHeight((int) Math.floor(options.getHeight() * scale));

			// Img = 100x100, width = 95x95, scale = 100/95, 1/scale > min
			// don't set minscale smaller than 1/scale
			// -> don't pick crops that need upscaling
			options.minScale(Math.min(options.getMaxScale(), Math.max(1.0f / scale, options.getMinScale())));

			if (options.isPrescale()) {
				prescale = Math.min(Math.max(256.0f / input.getWidth(), 256.0f / input.getHeight()), 1.0f);
				if (prescale < 1.0) {
					System.out.println("Prescaling");
					BufferedImage scaledInput = new BufferedImage((int) (input.getWidth() * prescale), (int) (input.getHeight() * prescale), options.getBufferedBitmapType());
					Graphics g = scaledInput.getGraphics();
					g.drawImage(input, 0, 0, scaledInput.getWidth(), scaledInput.getHeight(), 0, 0, input.getWidth(), input.getHeight(), null);
					g.dispose();

					input = scaledInput;
					options.cropWidth((int) (options.getCropWidth() * prescale));
					options.cropHeight((int) (options.getCropHeight() * prescale));
				} else {
					System.out.println("Not prescaling");
					prescale = 1.0f;
				}
			}
		}

		// analyse(options, input)
		Image inputI = new Image(input);
		Image outputI = new Image(input.getWidth(), input.getHeight());

		prepareCie(inputI);
		edgeDetect(inputI, outputI);
		skinDetect(inputI, outputI);
		saturationDetect(inputI, outputI);
		// applyBoosts()

		// scoreOutput = downSample
		BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), options.getBufferedBitmapType());
		output.setRGB(0, 0, input.getWidth(), input.getHeight(), outputI.getRGB(), 0, input.getWidth());

		BufferedImage score = new BufferedImage(input.getWidth() / options.getScoreDownSample(), input.getHeight() / options.getScoreDownSample(), options.getBufferedBitmapType());
		score.getGraphics().drawImage(output, 0, 0, score.getWidth(), score.getHeight(), 0, 0, output.getWidth(), output.getHeight(), null);
		Image scoreI = new Image(score);

		float topScore = Float.NEGATIVE_INFINITY;
		Crop topCrop = null;
		List<Crop> crops = generateCrops(input.getWidth(), input.getHeight());

		for (Crop crop : crops) {
			crop.score = score(scoreI, crop);
			if (crop.score.total > topScore) {
				topCrop = crop;
				topScore = crop.score.total;
			}
			crop.x = (int) Math.floor(crop.x / prescale);
			crop.y = (int) Math.floor(crop.y / prescale);
			crop.width = (int) Math.floor((crop.width / prescale));
			crop.height = (int) Math.floor(crop.height / prescale);
		}

		CropResult result = CropResult.newInstance(topCrop, crops, output, createCrop(input, topCrop));

		Graphics graphics = output.getGraphics();
		graphics.setColor(Color.cyan);
		if (topCrop != null)
			graphics.drawRect(topCrop.x, topCrop.y, topCrop.width, topCrop.height);

		return result;
	}

	public BufferedImage createCrop(BufferedImage input, Crop crop) {
		int tw = options.getCropWidth();
		int th = options.getCropHeight();
		BufferedImage image = new BufferedImage(tw, th, options.getBufferedBitmapType());
		image.getGraphics().drawImage(input, 0, 0, tw, th, crop.x, crop.y, crop.x + crop.width, crop.y + crop.height, null);
		return image;
	}

	private BufferedImage createScaleDown(BufferedImage image, float ratio) {
		BufferedImage scaled = new BufferedImage((int) (ratio * image.getWidth()), (int) (ratio * image.getHeight()), options.getBufferedBitmapType());
		scaled.getGraphics().drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), 0, 0, scaled.getWidth(), scaled.getHeight(), null);
		return scaled;
	}

	private List<Crop> generateCrops(int width, int height) {
		int minDimension = Math.min(width, height);
		int cropWidth = options.getCropWidth() == 0 ? minDimension : options.getCropWidth();
		int cropHeight = options.getCropHeight() == 0 ? minDimension : options.getCropHeight();

		List<Crop> crops = new ArrayList<>();
		for (float scale = options.getMaxScale(); scale >= options.getMinScale(); scale -= options.getScaleStep()) {
			int sampleW = (int) (cropWidth * scale);
			int sampleH = (int) (cropHeight * scale);
			for (int y = 0; y + sampleH <= height; y += options.getScoreDownSample()) {
				for (int x = 0; x + sampleW <= width; x += options.getScoreDownSample()) {
					crops.add(new Crop(x, y, sampleW, sampleH));
				}
			}
		}
		return crops;
	}

	private Score score(Image output, Crop crop) {
		Score score = new Score();
		int[] od = output.getRGB();
		int downSample = options.getScoreDownSample();
		float invDownSample = 1.0f / downSample;
		float outputHeightDownSample = output.height * downSample;
		float outputWidthDownSample = output.width * downSample;
		int outputWidth = output.width;

		for (int y = 0; y < outputHeightDownSample; y += downSample) {
			for (int x = 0; x < outputWidthDownSample; x += downSample) {
				int p = (int) (Math.floor(y * invDownSample) * outputWidth + Math.floor(x * invDownSample));
				float importance = importance(crop, x, y);
				float detail = (od[p] >> 8 & 0xff) / 255f;
				score.skin += (od[p] >> 16 & 0xff) / 255f * (detail + options.getSkinBias()) * importance;
				score.detail += detail * importance;
				score.saturation += (od[p] & 0xff) / 255f * (detail + options.getSaturationBias()) * importance;
			}
		}
		score.total = (score.detail * options.getDetailWeight() + score.skin * options.getSkinWeight() + score.saturation * options.getSaturationWeight()) / crop.width / crop.height;
		return score;
	}

	private float importance(Crop crop, int x, int y) {
		if (crop.x > x || x >= crop.x + crop.width || crop.y > y || y >= crop.y + crop.height)
			return options.getOutsideImportance();
		float fx = (float) (x - crop.x) / crop.width;
		float fy = (float) (y - crop.y) / crop.height;
		float px = Math.abs(0.5f - fx) * 2;
		float py = Math.abs(0.5f - fy) * 2;
		// distance from edg;
		float dx = Math.max(px - 1.0f + options.getEdgeRadius(), 0);
		float dy = Math.max(py - 1.0f + options.getEdgeRadius(), 0);
		float d = (dx * dx + dy * dy) * options.getEdgeWeight();
		d += (float) (1.4142135f - Math.sqrt(px * px + py * py));
		if (options.isRuleOfThirds()) {
			d += (Math.max(0, d + 0.5f) * 1.2f) * (thirds(px) + thirds(py));
		}
		return d;
	}

	static class Image {
		BufferedImage bufferedImage;
		int width, height;
		int[] data;

		public Image(int width, int height) {
			this.width = width;
			this.height = height;
			this.data = new int[width * height];
			for (int i = 0; i < this.data.length; i++)
				data[i] = 0xff000000;
		}

		public Image(BufferedImage bufferedImage) {
			this(bufferedImage.getWidth(), bufferedImage.getHeight());
			this.bufferedImage = bufferedImage;
			this.data = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
		}

		public int[] getRGB() {
			return data;
		}
	}

	private void prepareCie(Image i) {
		int[] id = i.getRGB();
		cd = new int[id.length];
		int w = i.width;
		int h = i.height;

		int p;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				p = y * w + x;
				cd[p] = cie(id[p]);
			}
		}
	}

	private void edgeDetect(Image i, Image o) {
		int[] od = o.getRGB();
		int w = i.width;
		int h = i.height;
		int p;
		int lightness;

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				p = y * w + x;
				if (x == 0 || x >= w - 1 || y == 0 || y >= h - 1) {
					lightness = 0;
				} else {
					lightness = cd[p] * 8
							- cd[p - w - 1]
							- cd[p - w]
							- cd[p - w + 1]
							- cd[p - 1]
							- cd[p + 1]
							- cd[p + w - 1]
							- cd[p + w]
							- cd[p + w + 1]
					;
				}

				od[p] = clamp(lightness) << 8 | (od[p] & 0xffff00ff);
			}
		}
	}

	private void skinDetect(Image i, Image o) {
		int[] id = i.getRGB();
		int[] od = o.getRGB();
		int w = i.width;
		int h = i.height;
		float invSkinThreshold = 255f / (1 - options.getSkinThreshold());

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = y * w + x;
				float lightness = cd[p] / 255f;
				float skin = calcSkinColor(id[p]);
				if (skin > options.getSkinThreshold() && lightness >= options.getSkinBrightnessMin() && lightness <= options.getSkinBrightnessMax()) {
					od[p] = ((Math.round((skin - options.getSkinThreshold()) * invSkinThreshold)) & 0xff) << 16 | (od[p] & 0xff00ffff);
				} else {
					od[p] &= 0xff00ffff;
				}
			}
		}
	}

	private void saturationDetect(Image i, Image o) {
		int[] id = i.getRGB();
		int[] od = o.getRGB();
		int w = i.width;
		int h = i.height;
		float invSaturationThreshold = 255f / (1 - options.getSaturationThreshold());

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = y * w + x;
				float lightness = cd[p] / 255f;
				float sat = saturation(id[p]);
				if (sat > options.getSaturationThreshold() && lightness >= options.getSaturationBrightnessMin() && lightness <= options.getSaturationBrightnessMax()) {
					od[p] = (Math.round((sat - options.getSaturationThreshold()) * invSaturationThreshold) & 0xff) | (od[p] & 0xffffff00);
				} else {
					od[p] &= 0xffffff00;
				}
			}
		}
	}

	private float calcSkinColor(int rgb) {
		int r = rgb >> 16 & 0xff;
		int g = rgb >> 8 & 0xff;
		int b = rgb & 0xff;

		float mag = (float) Math.sqrt(r * r + g * g + b * b);
		float rd = (r / mag - options.getSkinColor()[0]);
		float gd = (g / mag - options.getSkinColor()[1]);
		float bd = (b / mag - options.getSkinColor()[2]);
		return 1f - (float) Math.sqrt(rd * rd + gd * gd + bd * bd);
	}

	private int clamp(int v) {
		return Math.max(0, Math.min(v, 0xff));
	}

	private int cie(int rgb) {
		int r = rgb >> 16 & 0xff;
		int g = rgb >> 8 & 0xff;
		int b = rgb & 0xff;
		return Math.min(0xff, (int) (0.2126f * b + 0.7152f * g + 0.0722f * r + .5f));
	}

	private float saturation(int rgb) {
		float r = (rgb >> 16 & 0xff) / 255f;
		float g = (rgb >> 8 & 0xff) / 255f;
		float b = (rgb & 0xff) / 255f;

		float maximum = Math.max(r, Math.max(g, b));
		float minimum = Math.min(r, Math.min(g, b));
		if (maximum == minimum) {
			return 0;
		}
		float l = (maximum + minimum) / 2f;
		float d = maximum - minimum;
		return l > 0.5f ? d / (2f - maximum - minimum) : d / (maximum + minimum);
	}

	// gets value in the range of [0, 1] where 0 is the center of the pictures
	// returns weight of rule of thirds [0, 1]
	private float thirds(float x) {
		x = ((x - (1 / 3f) + 1.0f) % 2.0f * 0.5f - 0.5f) * 16f;
		return Math.max(1.0f - x * x, 0);
	}
}
