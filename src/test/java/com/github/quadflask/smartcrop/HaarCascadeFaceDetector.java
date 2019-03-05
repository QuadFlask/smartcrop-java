package com.github.quadflask.smartcrop;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RectVector;

import org.bytedeco.javacpp.opencv_java;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.Java2DFrameUtils;

import java.awt.image.BufferedImage;

import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.equalizeHist;

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
        Mat frame = Java2DFrameUtils.toMat(input);

        Mat grayFrame = new Mat();
        cvtColor(frame, grayFrame, COLOR_BGR2GRAY);
        equalizeHist(grayFrame, grayFrame);

        // detect faces
        RectVector faces = new RectVector();
        faceCascade.detectMultiScale(grayFrame, faces);

        for (long i = 0; i < faces.size(); i++) {
            Rect face = faces.get(i);
            consumer.accept(face.x(), face.y(), face.width(), face.height(), 1.0f);
        }

        grayFrame.release();
        frame.release();
    }

}
