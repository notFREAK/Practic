package com.lecturerecorder.services.video;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class VideoCaptureService implements Runnable {

    private VideoCapture videoCapture;
    private volatile boolean running = true;
    private int deviceIndex;
    private int width;
    private int height;
    private int fps;

    private BlockingQueue<Mat> frameQueue;

    public VideoCaptureService(int deviceIndex, int width, int height, int fps) {
        this.deviceIndex = deviceIndex;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.frameQueue = new LinkedBlockingQueue<>(10);

        videoCapture = new VideoCapture(deviceIndex);
        if (!videoCapture.isOpened()) {
            throw new RuntimeException("Не удалось открыть устройство видеозахвата с индексом: " + deviceIndex);
        }

        videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, width);
        videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, height);
        videoCapture.set(Videoio.CAP_PROP_FPS, fps);
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }

    public VideoCaptureService(int deviceIndex) {
        this.deviceIndex = deviceIndex;
        this.frameQueue = new LinkedBlockingQueue<>(10);

        videoCapture = new VideoCapture(deviceIndex);
        if (!videoCapture.isOpened()) {
            throw new RuntimeException("Не удалось открыть устройство видеозахвата с индексом: " + deviceIndex);
        }
    }

    @Override
    public void run() {
        Mat frame = new Mat();
        while (running) {
            if (videoCapture.read(frame)) {
                try {
                    Mat clonedFrame = frame.clone();
                    frameQueue.poll();
                    frameQueue.offer(clonedFrame, 33, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Не удалось захватить кадр с устройства " + deviceIndex);
                stop();
            }
        }
        videoCapture.release();
    }

    public void stop() {
        running = false;
    }

    public BlockingQueue<Mat> getFrameQueue() {
        return frameQueue;
    }
}
