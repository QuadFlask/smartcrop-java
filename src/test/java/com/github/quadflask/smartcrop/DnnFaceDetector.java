package com.github.quadflask.smartcrop;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_dnn.Net;
import org.bytedeco.javacpp.opencv_java;
import org.bytedeco.javacv.Java2DFrameUtils;

import java.awt.image.BufferedImage;

import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_dnn.blobFromImage;
import static org.bytedeco.javacpp.opencv_dnn.readNetFromCaffe;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class DnnFaceDetector implements FaceDetector {

    static {
        Loader.load(opencv_java.class);

        faceNet = readNetFromCaffe("src/test/resources/deploy.prototxt.txt", "src/test/resources/res10_300x300_ssd_iter_140000.caffemodel");
    }

    private static Net faceNet;

    @Override
    public void detect(BufferedImage input, FaceConsumer consumer) {
        Mat frame = Java2DFrameUtils.toMat(input);

        resize(frame, frame, new Size(300, 300));
        Mat blob = blobFromImage(frame, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0, 0.0), false, false, CV_32F);

        faceNet.setInput(blob);
        Mat output = faceNet.forward();

        Mat ne = new Mat(new Size(output.size(3), output.size(2)), CV_32F, output.ptr(0, 0));
        FloatIndexer srcIndexer = ne.createIndexer();

        for (int i = 0; i < output.size(3); i++) {
            float confidence = srcIndexer.get(i, 2);

            if (confidence > .6f) {
                float f1 = srcIndexer.get(i, 3);
                float f2 = srcIndexer.get(i, 4);
                float f3 = srcIndexer.get(i, 5);
                float f4 = srcIndexer.get(i, 6);

                int x = (int) (f1 * input.getWidth());
                int y = (int) (f2 * input.getHeight());
                int width = (int) ((f3 - f1) * input.getWidth());
                int height = (int) ((f4 - f2) * input.getHeight());

                consumer.accept(x, y, width, height, confidence);
            }
        }

        srcIndexer.release();
        ne.release();
        output.release();
        blob.release();
        frame.release();
    }

}
