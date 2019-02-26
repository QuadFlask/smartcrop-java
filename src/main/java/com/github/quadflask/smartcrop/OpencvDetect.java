package com.github.quadflask.smartcrop;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Slf4j
public class OpencvDetect {

    private static OpencvDetect instance = new OpencvDetect();
    private static CascadeClassifier cascadeClassifier;
    private static String frontalfacePath;

    static {
        try {
            String osName = System.getProperty("os.name");
            if (osName.startsWith("Windows")) {
                System.loadLibrary("lib"+Core.NATIVE_LIBRARY_NAME);
            } else if (osName.startsWith("Mac")) {
                System.load("/usr/local/lib/lib" + Core.NATIVE_LIBRARY_NAME + ".dylib");

            } else if (osName.startsWith("Linux")) {
                System.load("/usr/local/lib/lib" + Core.NATIVE_LIBRARY_NAME + ".so");
            }
        } catch (Throwable t) {
            log.error("Load OpenCV lib error.", t);
        }
    }


    public static OpencvDetect getInstance() {
        return instance;
    }


    public Rect[] detectFace(String imagePath) {
        Mat image = Imgcodecs.imread(imagePath);
        MatOfRect faceDetections = new MatOfRect();
        cascadeClassifier.detectMultiScale(image, faceDetections);
        Rect[] rects = faceDetections.toArray();
        return rects;
    }

    public Rect[] detectFace(byte[] imageBase64) {

        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(imageBase64));
            return detectFace(bi);

        } catch (Exception e) {
            log.error("dateface error.", e);
        }

        return null;
    }


    public Rect[] detectFace(BufferedImage bi) {

        Mat image = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        MatOfRect faceDetections = new MatOfRect();
        cascadeClassifier.detectMultiScale(image, faceDetections);
        Rect[] rects = faceDetections.toArray();
        return rects;
    }

    /**
     * the file is '~/opencvXX/haarcascades/haarcascade_frontalface_alt.xml"'
     *
     * @param frontalfacePath
     */
    public void SetFrontalFacePath(String frontalfacePath) {
        this.frontalfacePath = frontalfacePath;
        cascadeClassifier = new CascadeClassifier(frontalfacePath);
    }



    public
}
