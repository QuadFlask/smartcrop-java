package com.github.quadflask.smartcrop;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class OpencvDetect {

    private static OpencvDetect instance = new OpencvDetect();
    private static CascadeClassifier cascadeClassifier;
    private static String frontalfacePath;

    static {
        try {
            String osName = System.getProperty("os.name");
            if (osName.startsWith("Windows")) {
                System.loadLibrary("lib" + Core.NATIVE_LIBRARY_NAME);
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


    public Crop[] detectFace(String imagePath) {
        Mat image = Imgcodecs.imread(imagePath);
        MatOfRect faceDetections = new MatOfRect();
        cascadeClassifier.detectMultiScale(image, faceDetections);
        Rect[] rects = faceDetections.toArray();
        return toCrop(rects);
    }

    public Crop[] detectFace(byte[] imageBase64) {

        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(imageBase64));
            return detectFace(bi);

        } catch (Exception e) {
            log.error("dateface error.", e);
        }

        return null;
    }


    public Crop[] detectFace(BufferedImage bi) {

//        Mat image = Imgcodecs.imread("/Users/leeleon/github/smartcrop-java/src/test/resources/sample/test11.jpg");
//        Mat image = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        Mat image = img2Mat(bi);
        MatOfRect faceDetections = new MatOfRect();
        cascadeClassifier.detectMultiScale(image, faceDetections);
        Rect[] rects = faceDetections.toArray();
        return toCrop(rects);
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


    private Crop[] toCrop(Rect[] rect) {

        List<Crop> cropList = new ArrayList<>();
        if (rect != null) {

            Arrays.stream(rect).forEach(r -> cropList.add(new Crop(r.x,r.y,r.width,r.height)));

        }

        Crop[] crops = new Crop[cropList.size()];
        cropList.toArray(crops);
        return crops;
    }


    private Mat img2Mat(BufferedImage im)
    {
        // Convert INT to BYTE
        //im = new BufferedImage(im.getWidth(), im.getHeight(),BufferedImage.TYPE_3BYTE_BGR);
        // Convert bufferedimage to byte array
        byte[] pixels = ((DataBufferByte) im.getRaster().getDataBuffer())
                .getData();

        // Create a Matrix the same size of image
        Mat image = new Mat(im.getHeight(), im.getWidth(), CvType.CV_8UC3);
        // Fill Matrix with image values
        image.put(0, 0, pixels);

        return image;

    }


}
