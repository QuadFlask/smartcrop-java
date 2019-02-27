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

//        float wf = 256f / inputI.width;
//        float hf = 256f / inputI.height;
//        float prescale = Math.min(Math.max(wf, hf), 1f);
//        if (prescale < 1) {
//
//            options.setPrescale(prescale);
//            options.cropWidth(Math.round(options.getCropWidth() * prescale));
//            options.cropHeight(Math.round(options.getCropHeight() * prescale));
//
//            if (options.getBoost() != null && options.getBoost().length > 0) {
//                for (Crop c : options.getBoost()) {
//                    c.height = Math.round(c.height * prescale);
//                    c.width = Math.round(c.width * prescale);
//                    c.x = Math.round(c.x * prescale);
//                    c.y = Math.round(c.y * prescale);
//                }
//            }
//
//
//        }


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

//            crop.x = crop.x
//            crop.y = Math.round(crop.y / options.getPrescale());
//            crop.width = Math.round(crop.width / options.getPrescale());
//            crop.height = Math.round(crop.height / options.getPrescale());


            crop.x *= options.getScoreDownSample();
            crop.y *= options.getScoreDownSample();
            crop.width *= options.getScoreDownSample();
            crop.height *= options.getScoreDownSample();


            if (options.getBoost().length == 1) {
                Crop r = options.getBoost()[0];

                if (crop.x > r.x) {
                    crop.x = r.x;
                }

                if (crop.width < r.width) {
                    crop.width = r.width;
                }

                if (crop.y > r.y) {
                    crop.y = r.y;
                } else if ((crop.y + crop.height) < (r.y + r.height)) {
                    crop.height = crop.height + (r.y + r.height - crop.y - crop.height) * options.getScoreDownSample();
                }

                if (crop.height < r.height) {
                    crop.y = r.height;
                }
            }


        }

        CropResult result = CropResult.newInstance(topCrop, crops, output, createCrop(input, topCrop));

        Graphics graphics = output.getGraphics();
        graphics.setColor(Color.cyan);
        if (topCrop != null)
            graphics.drawRect(topCrop.x, topCrop.y, topCrop.width, topCrop.height);

        if (options.getBoost().length > 0) {
            for (Crop r : options.getBoost()) {
                graphics.drawRect(r.x, r.y, r.width, r.height);
            }
        }

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

    private List<Crop> crops(Image image) {
        List<Crop> crops = new ArrayList<>();
        int width = image.width;
        int height = image.height;
        int minDimension = Math.min(width, height);

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
        return crops;
    }

    private Score score(Image output, Crop crop) {
        Score score = new Score();
        int[] od = output.getRGB();
        int width = output.width * options.getScoreDownSample();
        int height = output.height * options.getScoreDownSample();

        float invDownSample = 1 / options.getScoreDownSample();

        for (int y = 0; y < height; y += options.getScoreDownSample()) {
            for (int x = 0; x < width; x += options.getScoreDownSample()) {
                int p = Math.round(y * invDownSample * width + x * invDownSample + 0.5f) * 4;
                float importance = importance(crop, x, y);

                float detail = (od[p] >> 8 & 0xff) / 255f;
                score.skin += (od[p] >> 16 & 0xff) / 255f * (detail + options.getSkinBias()) * importance;
                score.detail += detail * importance;
                score.saturation += (od[p] & 0xff) / 255f * (detail + options.getSaturationBias()) * importance;
                score.boost += (od[p] >> 24 & 0xff) / 255f * importance;
            }
        }


        score.total = (score.detail * options.getDetailWeight()
                + score.skin * options.getSkinWeight()
                + score.saturation * options.getSaturationWeight()
                + score.boost * options.getBoostWeight())
                / crop.width / crop.height;
        return score;
    }

    private float importance(Crop crop, int x, int y) {
        if (crop.x > x
                || x >= crop.x + crop.width
                || crop.y > y
                || y >= crop.y + crop.height)
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


    private void applyBoosts(Image o) {
        if (options.getBoost() == null || options.getBoost().length < 1) return;
        int[] od = o.data;
        for (int i = 0; i < o.width; i++) {
//            od[i] = (0 << 24) | (od[i] & 0xffffff);
            od[i] &= 0xffffff00;
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
                od[p] = (255 << 24) | (od[p] & 0xffffff);
//                od[p] = (255<<24) | (255<<16) | (255<<8) | 255;
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
