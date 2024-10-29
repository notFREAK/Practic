package com.lecturerecorder.services.video;

import com.lecturerecorder.services.presentation.PresentationOverlayService;
import com.lecturerecorder.services.telepromter.TeleprompterService;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Собственные классы


public class VideoProcessingService implements Runnable {

    private List<VideoCaptureService> captureServices;
    private PresentationOverlayService presentationOverlay;
    private TeleprompterService teleprompterService;
    private BlockingQueue<Mat> outputQueue = new LinkedBlockingQueue<>();

    private volatile boolean running = true;

    public VideoProcessingService(List<VideoCaptureService> captureServices,
                                  PresentationOverlayService presentationOverlay,
                                  TeleprompterService teleprompterService) {
        this.captureServices = captureServices;
        this.presentationOverlay = presentationOverlay;
        this.teleprompterService = teleprompterService;
    }

    @Override
    public void run() {
        while (running) {
            try {
                List<Mat> frames = new ArrayList<>();
                for (VideoCaptureService captureService : captureServices) {
                    Mat frame = captureService.getFrameQueue().poll();
                    if (frame != null) {
                        frames.add(frame);
                    }
                }

                if (!frames.isEmpty()) {
                    Mat combinedFrame = combineFrames(frames);

                    combinedFrame = presentationOverlay.overlaySlide(combinedFrame);

                    combinedFrame = teleprompterService.overlayTeleprompter(combinedFrame);

                    outputQueue.put(combinedFrame);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                showError("Ошибка", "Проблема при обработке видео: " + e.getMessage());
                stop();
            }
        }
    }

    public BlockingQueue<Mat> getOutputQueue() {
        return outputQueue;
    }

    public void stop() {
        running = false;
    }


    private Mat combineFrames(List<Mat> frames) {
        return frames.get(0);
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
