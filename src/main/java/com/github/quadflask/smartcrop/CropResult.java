package com.github.quadflask.smartcrop;

import java.awt.image.BufferedImage;
import java.util.List;

public class CropResult {
	public final Crop topCrop;
	public final List<Crop> crops;
	public final BufferedImage debugImage;
	public final BufferedImage resultImage;

	private CropResult(Crop topCrop, List<Crop> crops, BufferedImage debugImage, BufferedImage resultImage) {
		this.topCrop = topCrop;
		this.crops = crops;
		this.debugImage = debugImage;
		this.resultImage = resultImage;
	}

	static CropResult newInstance(Crop topCrop, List<Crop> crops, BufferedImage debugImage, BufferedImage resultImage) {
		return new CropResult(topCrop, crops, debugImage, resultImage);
	}
}
