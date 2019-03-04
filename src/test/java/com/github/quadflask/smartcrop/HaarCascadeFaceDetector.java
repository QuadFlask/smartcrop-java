package com.github.quadflask.smartcrop;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_java;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class HaarCascadeFaceDetector implements FaceDetector {

    static {
        Loader.load(opencv_java.class);

        CascadeClassifier classifier = new CascadeClassifier();
        classifier.load("src/test/resources/haarcascade_frontalface_default.xml");
        faceCascade = classifier;
    }

    private static CascadeClassifier faceCascade;

    @Override
    public void detect(BufferedImage input, FaceConsumer consumer) {
        int type = input.getType();
        if (type != BufferedImage.TYPE_3BYTE_BGR) {
            throw new RuntimeException("Unsupported image type: " + type);
        }

        Mat frame = new Mat(input.getHeight(), input.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) input.getRaster().getDataBuffer()).getData();
        frame.put(0, 0, data);

        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        // detect faces
        MatOfRect faces = new MatOfRect();
        faceCascade.detectMultiScale(grayFrame, faces, 1.1, 5);

        faces.toList().forEach(face -> {
            consumer.accept(face.x, face.y, face.width, face.height, 1.0f);
        });
    }

}
