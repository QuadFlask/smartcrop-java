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
        Image inputI = new Image(input);
        Image outputI = new Image(input.getWidth(), input.getHeight());

        prepareCie(inputI);
        edgeDetect(inputI, outputI);
        skinDetect(inputI, outputI);
        saturationDetect(inputI, outputI);
        applyBoosts(outputI);

        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), options.getBufferedBitmapType());
        output.setRGB(0, 0, input.getWidth(), input.getHeight(), outputI.getRGB(), 0, input.getWidth());

        BufferedImage score = new BufferedImage(input.getWidth() / options.getScoreDownSample(), input.getHeight() / options.getScoreDownSample(), options.getBufferedBitmapType());
        score.getGraphics().drawImage(output, 0, 0, score.getWidth(), score.getHeight(), 0, 0, output.getWidth(), output.getHeight(), null);
        Image scoreI = new Image(score);


        float topScore = Float.NEGATIVE_INFINITY;
        Crop topCrop = null;
        List<Crop> crops = crops(scoreI);

        for (Crop crop : crops) {
            crop.score = score(scoreI, crop);
            if (crop.score.total > topScore) {
                topCrop = crop;
                topScore = crop.score.total;
            }


            crop.x *= options.getScoreDownSample();
            crop.y *= options.getScoreDownSample();
            crop.width *= options.getScoreDownSample();
            crop.height *= options.getScoreDownSample();

        }

        CropResult result = CropResult.newInstance(topCrop, crops, output, createCrop(input, topCrop));
        if (options.isDebug()) {
            Graphics graphics = output.getGraphics();
            graphics.setColor(Color.cyan);
            if (topCrop != null)
                graphics.drawRect(topCrop.x, topCrop.y, topCrop.width, topCrop.height);


            if (options.getBoost() != null && options.getBoost().length > 0) {
                for (Crop r : options.getBoost()) {
                    graphics.drawRect(r.x, r.y, r.width, r.height);
                }
            }

            Graphics graphics1 = output.getGraphics();
            graphics1.setColor(Color.MAGENTA);
            for(Crop r: crops){
                graphics.drawRect(r.x, r.y, r.width, r.height);
            }

        }

        return result;
    }

    public BufferedImage createCrop(BufferedImage input, Crop crop) {

        int tw = crop.width;
        int th = crop.height;
        BufferedImage image = new BufferedImage(tw, th, options.getBufferedBitmapType());
        image.getGraphics().drawImage(input, 0, 0, tw, th, crop.x, crop.y, crop.x + crop.width, crop.y + crop.height, null);

        return image;
    }

    private BufferedImage createScaleDown(BufferedImage image, float ratio) {
        BufferedImage scaled = new BufferedImage((int) (ratio * image.getWidth()), (int) (ratio * image.getHeight()), options.getBufferedBitmapType());
        scaled.getGraphics().drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        return scaled;
    }

    private List<Crop> crops(Image image) {
        List<Crop> crops = new ArrayList<>();
        int width = image.width;
        int height = image.height;
        int minDimension = Math.min(width, height);
        int cropWidth = options.getCropWidth() > minDimension ? minDimension : options.getCropWidth();
        int cropHeight = options.getCropHeight() > minDimension ? minDimension : options.getCropHeight();


        if (options.getBoost() != null && options.getBoost().length > 0) {
            for (Crop r : options.getBoost()) {
                int tx = Math.min(r.x, (width * options.getScoreDownSample() - r.x - r.width));
                int ty = Math.min(r.y, (height * options.getScoreDownSample() - r.y - r.height));
                int ttx = Math.min(tx, ty);
                crops.add(new Crop(
                        (r.x - ttx) / options.getScoreDownSample(),
                        (r.y - ty) / options.getScoreDownSample(),
                        Math.round((r.width + ttx * 2) / options.getScoreDownSample()),
                        Math.round((r.height + ty * 2) / options.getScoreDownSample())));
            }
        }

            for (float scale = options.getMaxScale();
                 scale >= options.getMinScale();
                 scale -= options.getScaleStep()) {

                for (int y = 0; y + cropHeight * scale <= height; y += options.getScoreDownSample()) {
                    for (int x = 0; x + cropWidth * scale <= width; x += options.getScoreDownSample()) {
                        crops.add(new Crop(
                                x,
                                y,
                                (int) (cropWidth * scale),
                                (int) (cropHeight * scale)));
                    }
                }
            }

//        else {
        for (float scale = options.getMaxScale();
             scale >= options.getMinScale();
             scale -= options.getScaleStep()) {

            for (int y = 0; y + minDimension * scale <= height; y += options.getScoreDownSample()) {
                for (int x = 0; x + minDimension * scale <= width; x += options.getScoreDownSample()) {
                    crops.add(new Crop(
                            x,
                            y,
                            (int) (minDimension * scale),
                            (int) (minDimension * scale)));
                }
            }
        }
//        }


        return crops;
    }

    private Score score(Image output, Crop crop) {

        Score result = new Score();
        int[] od = output.getRGB();
        int downSample = options.getScoreDownSample();
        float invDownSample = 1f / downSample;
        int outputHeightDownSample = output.height * downSample;
        int outputWidthDownSample = output.width * downSample;
        int outputWidth = output.width;

//        for (int y = 0; y < output.height - downSample; y += downSample) {
//            for (int x = 0; x < output.width - downSample; x += downSample) {
//                int p = y * outputWidth + x;
//                float i = importance(crop, x, y);
//                float detail = getGreen(od[p]) / 255f;
//                result.skin += getRed(od[p]) / 255f * (detail + options.getSkinBias()) * i;
//                result.detail += detail * i;
//                result.saturation += getBlue(od[p]) / 255f * (detail + options.getSaturationBias()) * i;
//                result.boost += getAlpha(od[p]) / 255f * i;
//            }
//        }
//
        for (int y = 0; y < outputHeightDownSample; y += downSample) {
            for (int x = 0; x < outputWidthDownSample; x += downSample) {
                int p = Math.round(y * invDownSample * outputWidth + x * invDownSample);
                float i = importance(crop, x, y);
                float detail = getGreen(od[p]) / 255f;
                result.skin += getRed(od[p]) / 255f * (detail + options.getSkinBias()) * i;
                result.detail += detail * i;
                result.saturation += getBlue(od[p]) / 255f * (detail + options.getSaturationBias()) * i;
                result.boost += getAlpha(od[p]) / 255f * i;
            }
        }


        /**
         Score score = new Score();
         int[] od = output.getRGB();
         int width = output.width ;
         //        int width = output.width * options.getScoreDownSample();
         int height = output.height;
         //        int height = output.height * options.getScoreDownSample();

         //        float invDownSample = 1f / options.getScoreDownSample();
         float invDownSample = 1f ;
         System.out.println(od.length + " "+ (Math.round((height-1f) * invDownSample * output.width + (width-1f) * invDownSample)));
         for (int y = 0; y < height; y += options.getScoreDownSample()) {
         for (int x = 0; x < width; x += options.getScoreDownSample()) {
         //                int p = Math.round(y * invDownSample * output.width + x * invDownSample) * 4;
         int p = Math.round(y * invDownSample * width + x * invDownSample);

         float importance = importance(crop, x, y);
         //                if (importance > 0){
         //                    System.out.println(importance);
         //                }
         float detail = (od[p] >> 8 & 0xff) / 255f;
         score.skin += (od[p] >> 16 & 0xff) / 255f * (detail + options.getSkinBias()) * importance;
         score.detail += detail * importance;
         score.saturation += (od[p] & 0xff) / 255f * (detail + options.getSaturationBias()) * importance;
         score.boost += (od[p] >> 24 & 0xff) / 255f * importance;
         }
         }
         */

        result.total = (result.detail * options.getDetailWeight()
                + result.skin * options.getSkinWeight()
                + result.saturation * options.getSaturationWeight()
                + result.boost * options.getBoostWeight())
                / (crop.width + crop.height);
        return result;
    }

    private float importance(Crop crop, int x, int y) {
        if (crop.x > x
                || x >= crop.x + crop.width
                || crop.y > y
                || y >= crop.y + crop.height)
            return options.getOutsideImportance();

        float fx = (0.0f + x - crop.x) / crop.width;
        float fy = (0.0f + y - crop.y) / crop.height;
        float px = Math.abs(0.5f - fx) * 2;
        float py = Math.abs(0.5f - fy) * 2;
        // distance from edg;
        float dx = Math.max(px - 1.0f + options.getEdgeRadius(), 0f);
        float dy = Math.max(py - 1.0f + options.getEdgeRadius(), 0f);
        float d = (dx * dx + dy * dy) * options.getEdgeWeight();
//        float s = (float) (1.4142135f - Math.sqrt(px * px + py * py));
        float s = (float) (1.41f - Math.sqrt(px * px + py * py));
        if (options.isRuleOfThirds()) {
            s += (Math.max(0f, s + d + 0.5f) * 1.2f) * (thirds(px) + thirds(py));
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
                            - cd[p + w + 1];
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
                if (skin > options.getSkinThreshold() && (lightness >= options.getSkinBrightnessMin() && lightness <= options.getSkinBrightnessMax())) {
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


    private void applyBoosts(Image o) {
        if (options.getBoost() == null || options.getBoost().length < 1) return;
        int[] od = o.data;
        for (int i = 0; i < o.width; i++) {
            od[i] = setAlpha(od[i], 0);
        }

        for (int i = 0; i < options.getBoost().length; i++) {
            applyBoost(options.getBoost()[i], o);
        }
    }

    private void applyBoost(Crop boost, Image o) {

        int[] od = o.data;
        int w = o.width;
        int x0 = boost.x;
        int x1 = boost.x + boost.width;
        int y0 = boost.y;
        int y1 = boost.y + boost.height;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int p = (y * w + x);
                od[p] = setAlpha(od[p], getAlpha(od[p]) + 255);
                od[p] = setGreen(od[p], 255);
//                od[p] = cie(od[p]);
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
        int r = getRed(rgb);
        int g = getGreen(rgb);
        int b = getBlue(rgb);
        return Math.min(0xff, (int) (0.2126f * b + 0.7152f * g + 0.0722f * r + .5f));
//        return Math.min(0xff, (int) (0.5126f * b + 0.7152f * g + 0.0722f * r + .5f));
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
        x = (((x - (1f / 3f) + 1.0f) % 2.0f) * 0.5f - 0.5f) * 16f;
        return Math.max(1.0f - x * x, 0f);
    }


    private int setAlpha(int pixel, int alpha) {
        return (alpha << 24) | (getRed(pixel) << 16) | (getGreen(pixel) << 8) | getBlue(pixel);
//        return (alpha << 24) | pixel & 0x00ffffff;
    }


    private int setRed(int pixel, int red) {
        return (getAlpha(pixel) << 24) | (red << 16) | (getGreen(pixel) << 8) | getBlue(pixel);
    }

    private int setGreen(int pixel, int green) {
        return (getAlpha(pixel) << 24) | (getRed(pixel) << 16) | (green << 8) | getBlue(pixel);
    }

    private int setBlue(int pixel, int blue) {
        return (getAlpha(pixel) << 24) | (getRed(pixel) << 16) | (getGreen(pixel) << 8) | blue;
    }


    //+3
    private int getAlpha(int pixel) {
        return (pixel >> 24) & 0xff;
    }

    //0
    private int getRed(int pixel) {
        return (pixel >> 16) & 0xff;
    }

    // +1
    private int getGreen(int pixel) {
        return (pixel >> 8) & 0xff;
    }

    // +2
    private int getBlue(int pixel) {
        return (pixel) & 0xff;
    }


}
