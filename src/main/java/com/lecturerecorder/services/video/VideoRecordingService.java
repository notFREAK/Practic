package com.lecturerecorder.services.video;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.BlockingQueue;

public class VideoRecordingService implements Runnable {

    private BlockingQueue<Mat> frameQueue;
    private volatile boolean running = true;
    private VideoWriter videoWriter;
    private Size frameSize;

    public VideoRecordingService(VideoCaptureService videoCaptureService, BlockingQueue<Mat> frameQueue, String outputPath) {
        this.frameQueue = frameQueue;
        this.frameSize = new Size(videoCaptureService.getWidth(),videoCaptureService.getHeight());

        videoWriter = new VideoWriter(formatPath(outputPath), VideoWriter.fourcc('M', 'J','P','G'), 30, frameSize, true);
        if (!videoWriter.isOpened()) {
            throw new RuntimeException("Не удалось открыть VideoWriter");
        }
    }

    private String formatPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("outputPath не может быть null");
        }
        return path.replace("\\", "\\\\");
    }
    @Override
    public void run() {
        while (running) {
            try {
                Mat frame = frameQueue.take();

                if (frame.width() != frameSize.width || frame.height() != frameSize.height) {
                    Mat resizedFrame = new Mat();
                    Imgproc.resize(frame, resizedFrame, frameSize);
                    frame = resizedFrame;
                }

                videoWriter.write(frame);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        videoWriter.release();
    }

    public void stop() {
        running = false;
    }
}
