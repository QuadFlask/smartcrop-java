package com.github.quadflask.smartcrop;

import java.awt.image.BufferedImage;
import java.util.List;

public class CropResult {
	public final Crop topCrop;
	public final List<Crop> crops;
	public final BufferedImage debugImage;

	private CropResult(Crop topCrop, List<Crop> crops, BufferedImage debugImage) {
		this.topCrop = topCrop;
		this.crops = crops;
		this.debugImage = debugImage;
	}

	static CropResult newInstance(Crop topCrop, List<Crop> crops, BufferedImage debugImage) {
		return new CropResult(topCrop, crops, debugImage);
	}
}
