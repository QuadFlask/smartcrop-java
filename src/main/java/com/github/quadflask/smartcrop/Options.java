package com.github.quadflask.smartcrop;

/**
 * Created by flask on 2015. 10. 30..
 */
public class Options {
    public static final Options DEFAULT = new Options();

    private int cropWidth = 0;
    private int cropHeight = 0;
    private float detailWeight = .2f;
    private float[] skinColor = {0.7f, 0.57f, 0.44f};
    private float skinBias = .01f;
    private float skinBrightnessMin = 0.2f;
    private float skinBrightnessMax = 1.0f;
    private float skinThreshold = 0.8f;
    private float skinWeight = 1.8f;
    private float saturationBrightnessMin = 0.05f;
    private float saturationBrightnessMax = 0.9f;
    private float saturationThreshold = 0.4f;
    private float saturationBias = 0.2f;
    private float saturationWeight = 0.3f;
    // step * minscale rounded down to the next power of two should be good
    private int scoreDownSample = 8;
    //	private int step = 8;
    private float scaleStep = 0.1f;
    private float minScale = 0.8f;
    private float maxScale = 1.0f;
    private float edgeRadius = 0.4f;
    private float edgeWeight = -20f;
    private float outsideImportance = -.5f;
    private boolean ruleOfThirds = false;

    public int getCropWidth() {
        return cropWidth;
    }

    public void setCropWidth(int cropWidth) {
        this.cropWidth = cropWidth;
    }

    public int getCropHeight() {
        return cropHeight;
    }

    public void setCropHeight(int cropHeight) {
        this.cropHeight = cropHeight;
    }

    public float getDetailWeight() {
        return detailWeight;
    }

    public void setDetailWeight(float detailWeight) {
        this.detailWeight = detailWeight;
    }

    public float[] getSkinColor() {
        return skinColor;
    }

    public void setSkinColor(float[] skinColor) {
        this.skinColor = skinColor;
    }

    public float getSkinBias() {
        return skinBias;
    }

    public void setSkinBias(float skinBias) {
        this.skinBias = skinBias;
    }

    public float getSkinBrightnessMin() {
        return skinBrightnessMin;
    }

    public void setSkinBrightnessMin(float skinBrightnessMin) {
        this.skinBrightnessMin = skinBrightnessMin;
    }

    public float getSkinBrightnessMax() {
        return skinBrightnessMax;
    }

    public void setSkinBrightnessMax(float skinBrightnessMax) {
        this.skinBrightnessMax = skinBrightnessMax;
    }

    public float getSkinThreshold() {
        return skinThreshold;
    }

    public void setSkinThreshold(float skinThreshold) {
        this.skinThreshold = skinThreshold;
    }

    public float getSkinWeight() {
        return skinWeight;
    }

    public void setSkinWeight(float skinWeight) {
        this.skinWeight = skinWeight;
    }

    public float getSaturationBrightnessMin() {
        return saturationBrightnessMin;
    }

    public void setSaturationBrightnessMin(float saturationBrightnessMin) {
        this.saturationBrightnessMin = saturationBrightnessMin;
    }

    public float getSaturationBrightnessMax() {
        return saturationBrightnessMax;
    }

    public void setSaturationBrightnessMax(float saturationBrightnessMax) {
        this.saturationBrightnessMax = saturationBrightnessMax;
    }

    public float getSaturationThreshold() {
        return saturationThreshold;
    }

    public void setSaturationThreshold(float saturationThreshold) {
        this.saturationThreshold = saturationThreshold;
    }

    public float getSaturationBias() {
        return saturationBias;
    }

    public void setSaturationBias(float saturationBias) {
        this.saturationBias = saturationBias;
    }

    public float getSaturationWeight() {
        return saturationWeight;
    }

    public void setSaturationWeight(float saturationWeight) {
        this.saturationWeight = saturationWeight;
    }

    public int getScoreDownSample() {
        return scoreDownSample;
    }

    public void setScoreDownSample(int scoreDownSample) {
        this.scoreDownSample = scoreDownSample;
    }

    public float getScaleStep() {
        return scaleStep;
    }

    public void setScaleStep(float scaleStep) {
        this.scaleStep = scaleStep;
    }

    public float getMinScale() {
        return minScale;
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public float getEdgeRadius() {
        return edgeRadius;
    }

    public void setEdgeRadius(float edgeRadius) {
        this.edgeRadius = edgeRadius;
    }

    public float getEdgeWeight() {
        return edgeWeight;
    }

    public void setEdgeWeight(float edgeWeight) {
        this.edgeWeight = edgeWeight;
    }

    public float getOutsideImportance() {
        return outsideImportance;
    }

    public void setOutsideImportance(float outsideImportance) {
        this.outsideImportance = outsideImportance;
    }

    public boolean isRuleOfThirds() {
        return ruleOfThirds;
    }

    public void setRuleOfThirds(boolean ruleOfThirds) {
        this.ruleOfThirds = ruleOfThirds;
    }
}