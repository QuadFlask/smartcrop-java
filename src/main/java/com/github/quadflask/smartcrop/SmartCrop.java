package com.github.quadflask.smartcrop;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BandCombineOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by flask on 2015. 10. 30..
 */
public class SmartCrop {
	private double prescale = 1.0;
	private BufferedImage input;
	private BufferedImage scaledInput;
	private BufferedImage score;
	private BufferedImage scoreOutput;

	private SmartCrop() {
	}

	public static SmartCrop analyze(Options options, BufferedImage input) {
		return new SmartCrop().doAnalyze(options, input);
	}

	private SmartCrop doAnalyze(Options options, BufferedImage original) {
		input = original;
		scaledInput = original;

		if (options.isPrescale()) {
			prescale = Math.min(Math.max(256.0 / input.getWidth(), 256.0 / input.getHeight()), 1.0);
			if (prescale < 1.0) {
				scaledInput = createScaleDown(original, prescale);

				for (Boost boost : options.getBoost()) {
					boost.x = (int) (boost.x * this.prescale);
					boost.y = (int) (boost.y * this.prescale);
					boost.width = (int) (boost.width * this.prescale);
					boost.height = (int) (boost.height * this.prescale);
				}
			}
		}

		// analyse(options, input)
		Image inputI = new Image(scaledInput);
		Image outputI = new Image(scaledInput.getWidth(), scaledInput.getHeight());

		int[] cie = generateCIE(inputI);
		edgeDetect(inputI, outputI, cie);
		skinDetect(options, inputI, outputI, cie);
		saturationDetect(options, inputI, outputI, cie);
		applyBoosts(options, outputI);

		scoreOutput = new BufferedImage(scaledInput.getWidth(), scaledInput.getHeight(), BufferedImage.TYPE_INT_ARGB);
		scoreOutput.setRGB(0, 0, scaledInput.getWidth(), scaledInput.getHeight(), outputI.getRGB(), 0, scaledInput.getWidth());
		score = downSample(options, scoreOutput);

		return this;
	}

	public CropResult generateCrops(Options options) {
		if (options.getAspect() != 0.0f) {
			options.width(options.getAspect());
			options.height(1.0f);
		}

		// calculate desired crop dimensions based on the image size
		if (options.getWidth() != 0.0f && options.getHeight() != 0.0f) {
			float scale = Math.min(input.getWidth() / options.getWidth(), input.getHeight() / options.getHeight());
			options.cropWidth((int) (options.getWidth() * scale));
			options.cropHeight((int) (options.getHeight() * scale));

			// Img = 100x100, width = 95x95, scale = 100/95, 1/scale > min
			// don't set minscale smaller than 1/scale
			// -> don't pick crops that need upscaling
			options.minScale(Math.min(options.getMaxScale(), Math.max(1.0f / scale, options.getMinScale())));
		}

		if (options.isPrescale()) {
			options.cropWidth((int) (options.getCropWidth() * prescale));
			options.cropHeight((int) (options.getCropHeight() * prescale));
		}

		Image scoreI = new Image(score);

		float topScore = Float.NEGATIVE_INFINITY;
		Crop topCrop = null;
		List<Crop> crops = generateCrops(options, scaledInput.getWidth(), scaledInput.getHeight());

		for (Crop crop : crops) {
			crop.score = score(options, scoreI, crop);
			if (crop.score.total > topScore) {
				topCrop = crop;
				topScore = crop.score.total;
			}
			if (options.isPrescale()) {
				crop.x = (int) (crop.x / prescale);
				crop.y = (int) (crop.y / prescale);
				crop.width = (int) (crop.width / prescale);
				crop.height = (int) (crop.height / prescale);
			}
		}

		return CropResult.newInstance(topCrop, crops);
	}

	private BufferedImage downSample(Options options, BufferedImage input) {
		int factor = options.getScoreDownSample();
		int[] idata = input.getRGB(0, 0, input.getWidth(), input.getHeight(), null, 0, input.getWidth());
		int iwidth = input.getWidth();
		int width = input.getWidth() / factor;
		int height = input.getHeight() / factor;

		BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] data = output.getRGB(0, 0, width, height, null, 0, width);
		float ifactor2 = 1.0f / (factor * factor);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = (y * width + x);

				int r = 0, g = 0, b = 0, a = 0, mr = 0, mg = 0;

				for (int v = 0; v < factor; v++) {
					for (int u = 0; u < factor; u++) {
						int rgb = idata[(y * factor + v) * iwidth + (x * factor + u)];

						a += rgb >> 24 & 0xff;
						r += rgb >> 16 & 0xff;
						g += rgb >> 8 & 0xff;
						b += rgb & 0xff;

						mr = Math.max(mr, rgb >> 16 & 0xff);
						mg = Math.max(mg, rgb >> 8 & 0xff);
						// unused
						// mb = Math.max(mb, rgb & 0xff);
					}
				}

				// this is some funky magic to preserve detail a bit more for
				// skin (r) and detail (g). Saturation (b) does not get this boost.
				data[i] = Math.min(255, (int) (a * ifactor2)) << 24 |
						  Math.min(255, (int) (r * ifactor2 * 0.5 + mr * 0.5)) << 16 |
						  Math.min(255, (int) (g * ifactor2 * 0.7 + mg * 0.3)) << 8 |
						  Math.min(255, (int) (b * ifactor2));
			}
		}

		output.setRGB(0, 0, width, height, data, 0, width);
		return output;
	}

	private BufferedImage createScaleDown(BufferedImage image, double ratio) {
		BufferedImage scaled = new BufferedImage((int) (image.getWidth() * ratio), (int) (image.getHeight() * ratio), BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = (Graphics2D) scaled.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), 0, 0, image.getWidth(), image.getHeight(), null);
		g.dispose();

		return scaled;
	}

	private List<Crop> generateCrops(Options options, int width, int height) {
		int minDimension = Math.min(width, height);
		int cropWidth = options.getCropWidth() == 0 ? minDimension : options.getCropWidth();
		int cropHeight = options.getCropHeight() == 0 ? minDimension : options.getCropHeight();

		List<Crop> crops = new ArrayList<>();
		for (float scale = options.getMaxScale(); scale >= options.getMinScale(); scale -= options.getScaleStep()) {
			int sampleW = (int) (cropWidth * scale);
			int sampleH = (int) (cropHeight * scale);
			for (int y = 0; y + sampleH <= height; y += options.getStep()) {
				for (int x = 0; x + sampleW <= width; x += options.getStep()) {
					crops.add(new Crop(x, y, sampleW, sampleH));
				}
			}
		}
		return crops;
	}

	private Score score(Options options, Image output, Crop crop) {
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
				float importance = importance(options, crop, x, y);
				float detail = (od[p] >> 8 & 0xff) / 255f;
				score.skin += (od[p] >> 16 & 0xff) / 255f * (detail + options.getSkinBias()) * importance;
				score.detail += detail * importance;
				score.saturation += (od[p] & 0xff) / 255f * (detail + options.getSaturationBias()) * importance;
				score.boost += (od[p] >> 24 & 0xff) / 255f * importance;
			}
		}
		score.total = (
				score.detail * options.getDetailWeight() +
				score.skin * options.getSkinWeight() +
				score.saturation * options.getSaturationWeight() +
				score.boost * options.getBoostWeight()
		) / (crop.width * crop.height);
		return score;
	}

	private float importance(Options options, Crop crop, int x, int y) {
		if (crop.x > x || x >= crop.x + crop.width || crop.y > y || y >= crop.y + crop.height) {
			return options.getOutsideImportance();
		}

		float fx = (float) (x - crop.x) / crop.width;
		float fy = (float) (y - crop.y) / crop.height;
		float px = Math.abs(0.5f - fx) * 2;
		float py = Math.abs(0.5f - fy) * 2;
		// distance from edg;
		float dx = Math.max(px - 1.0f + options.getEdgeRadius(), 0);
		float dy = Math.max(py - 1.0f + options.getEdgeRadius(), 0);
		float d = (dx * dx + dy * dy) * options.getEdgeWeight();
		float s = (float) (1.41f - Math.sqrt(px * px + py * py));
		if (options.isRuleOfThirds()) {
			s += Math.max(0, s + d + 0.5) * 1.2f * (thirds(px) + thirds(py));
		}
		return s + d;
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
				data[i] = 0x00000000;
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

	private int[] generateCIE(Image i) {
		int[] id = i.getRGB();
		int[] cie = new int[id.length];
		int w = i.width;
		int h = i.height;

		int p;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				p = y * w + x;

				int rgb = id[p];
				int r = rgb >> 16 & 0xff;
				int g = rgb >> 8 & 0xff;
				int b = rgb & 0xff;
				cie[p] = Math.min(0xff, (int) (0.5126f * b + 0.7152f * g + 0.0722f * r));
			}
		}

		return cie;
	}

	private void edgeDetect(Image i, Image o, int[] cie) {
		int[] od = o.getRGB();
		int w = i.width;
		int h = i.height;
		int p;

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				p = y * w + x;
				int lightness;
				if (x == 0 || x >= w - 1 || y == 0 || y >= h - 1) {
					lightness = cie[p];
				} else {
					lightness = cie[p] * 4 -
								cie[p - w] -
								cie[p - 1] -
								cie[p + 1] -
								cie[p + w]
					;
				}

				od[p] = clamp(lightness) << 8 | (od[p] & 0xffff00ff);
			}
		}
	}

	private void skinDetect(Options options, Image i, Image o, int[] cie) {
		int[] id = i.getRGB();
		int[] od = o.getRGB();
		int w = i.width;
		int h = i.height;
		float invSkinThreshold = 255f / (1 - options.getSkinThreshold());

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = y * w + x;
				float lightness = cie[p] / 255f;
				float skin = calcSkinColor(id[p], options.getSkinColor());
				if (skin > options.getSkinThreshold() && lightness >= options.getSkinBrightnessMin() && lightness <= options.getSkinBrightnessMax()) {
					od[p] = clamp((int) ((skin - options.getSkinThreshold()) * invSkinThreshold)) << 16 | (od[p] & 0xff00ffff);
				} else {
					od[p] &= 0xff00ffff;
				}
			}
		}
	}

	private void saturationDetect(Options options, Image i, Image o, int[] cie) {
		int[] id = i.getRGB();
		int[] od = o.getRGB();
		int w = i.width;
		int h = i.height;
		float invSaturationThreshold = 255f / (1 - options.getSaturationThreshold());

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = y * w + x;
				float lightness = cie[p] / 255f;
				float sat = saturation(id[p]);
				if (sat > options.getSaturationThreshold() && lightness >= options.getSaturationBrightnessMin() && lightness <= options.getSaturationBrightnessMax()) {
					od[p] = clamp((int) ((sat - options.getSaturationThreshold()) * invSaturationThreshold)) | (od[p] & 0xffffff00);
				} else {
					od[p] &= 0xffffff00;
				}
			}
		}
	}

	private float calcSkinColor(int rgb, float[] skinColor) {
		int r = rgb >> 16 & 0xff;
		int g = rgb >> 8 & 0xff;
		int b = rgb & 0xff;

		float mag = (float) Math.sqrt(r * r + g * g + b * b);
		float rd = (r / mag - skinColor[0]);
		float gd = (g / mag - skinColor[1]);
		float bd = (b / mag - skinColor[2]);
		return 1f - (float) Math.sqrt(rd * rd + gd * gd + bd * bd);
	}

	private void applyBoosts(Options options, Image o) {
		if (options.getBoost().isEmpty()) {
			return;
		}

		int w = o.width;
		int[] od = o.getRGB();
		options.getBoost().forEach(boost -> {
			int x0 = boost.x;
			int y0 = boost.y;
			int x1 = boost.x + boost.width;
			int y1 = boost.y + boost.height;
			int weight = (int) (boost.weight * 255);
			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					int i = y * w + x;
					int v = od[i] >> 24 & 0xff;
					od[i] = clamp(v + weight) << 24 | (od[i] & 0x00ffffff);
				}
			}
		});
	}

	private int clamp(int v) {
		return Math.max(0, Math.min(v, 0xff));
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

	public BufferedImage createDebugOutput(Crop topCrop, List<Boost> boosts) {
		BufferedImage output = new BufferedImage(scoreOutput.getWidth(), scoreOutput.getHeight(), BufferedImage.TYPE_INT_RGB);

		// Drop alpha channel from debug output
		BandCombineOp filterAlpha = new BandCombineOp(
				// RGBA -> RGB
				new float[][] {
						{1.0f, 0.0f, 0.0f, 0.0f},
						{0.0f, 1.0f, 0.0f, 0.0f},
						{0.0f, 0.0f, 1.0f, 0.0f}
				}, null
		);
		filterAlpha.filter(scoreOutput.getRaster(), output.getRaster());

		Graphics2D g = (Graphics2D) output.getGraphics();

		// Draw crop area
		if (topCrop != null) {
			g.setColor(Color.cyan);
			g.drawRect((int) (topCrop.x * prescale), (int) (topCrop.y * prescale), (int) (topCrop.width * prescale), (int) (topCrop.height * prescale));
		}

		// Draw boost areas
		g.setColor(Color.WHITE);
		boosts.forEach(b -> g.drawRect(b.x, b.y, b.width, b.height));

		g.dispose();

		return output;
	}
}
