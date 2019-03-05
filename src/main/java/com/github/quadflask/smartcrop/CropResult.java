package com.github.quadflask.smartcrop;

import java.util.List;

public class CropResult {
	public final Crop topCrop;
	public final List<Crop> crops;

	private CropResult(Crop topCrop, List<Crop> crops) {
		this.topCrop = topCrop;
		this.crops = crops;
	}

	static CropResult newInstance(Crop topCrop, List<Crop> crops) {
		return new CropResult(topCrop, crops);
	}
}
