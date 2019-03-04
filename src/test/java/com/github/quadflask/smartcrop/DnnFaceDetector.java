package com.github.quadflask.smartcrop;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_dnn;
import org.bytedeco.javacpp.opencv_java;
import org.bytedeco.javacv.Java2DFrameUtils;

import java.awt.image.BufferedImage;

import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_dnn.blobFromImage;
import static org.bytedeco.javacpp.opencv_dnn.readNetFromCaffe;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class DnnFaceDetector {

    static {
        Loader.load(opencv_java.class);

        faceNet = readNetFromCaffe("src/test/resources/deploy.prototxt.txt", "src/test/resources/res10_300x300_ssd_iter_140000.caffemodel");
    }

    private static opencv_dnn.Net faceNet;

    public void detect(BufferedImage input, FaceConsumer consumer) {
        opencv_core.Mat frame = Java2DFrameUtils.toMat(input);

        resize(frame, frame, new opencv_core.Size(300, 300));
        opencv_core.Mat blob = blobFromImage(frame, 1.0, new opencv_core.Size(300, 300), new opencv_core.Scalar(104.0, 177.0, 123.0, 0.0), false, false, CV_32F);

        faceNet.setInput(blob);
        opencv_core.Mat output = faceNet.forward();

        opencv_core.Mat ne = new opencv_core.Mat(new opencv_core.Size(output.size(3), output.size(2)), CV_32F, output.ptr(0, 0));
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

    public interface FaceConsumer {
        void accept(int x, int y, int width, int height, float confidence);
    }

}
