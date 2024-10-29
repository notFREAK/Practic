package com.lecturerecorder.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import com.lecturerecorder.services.video.VideoCaptureService;
import com.lecturerecorder.utils.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opencv.core.Mat;

public class TeleprompterWindowController {

    @FXML
    private ImageView previewImageView;

    @FXML
    private ScrollPane textScrollPane;

    @FXML
    private VBox textContainer;

    @FXML
    private Text teleprompterText;

    private VideoCaptureService mainCaptureService;

    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    private double scrollSpeed = 1.0;

    private volatile boolean running = true;

    public void setTeleprompterText(String text) {
        teleprompterText.setText(text);
    }

    public void setFontFamily(String fontFamily) {
        teleprompterText.setFont(javafx.scene.text.Font.font(fontFamily, teleprompterText.getFont().getSize()));
    }

    public void setFontSize(int fontSize) {
        teleprompterText.setFont(javafx.scene.text.Font.font(teleprompterText.getFont().getFamily(), fontSize));
    }

    public void setFontColor(Color color) {
        teleprompterText.setFill(color);
    }

    public void setScrollSpeed(double speed) {
        this.scrollSpeed = speed;
    }

    public void setMainCaptureService(VideoCaptureService captureService) {
        this.mainCaptureService = captureService;
    }

    @FXML
    private void initialize() {
        textScrollPane.setStyle("-fx-background-color: transparent;");
        textContainer.setStyle("-fx-background-color: transparent;");
        teleprompterText.setStyle("-fx-background-color: transparent;");
        startScrollingText();
    }

    private void startScrollingText() {
        executorService.submit(() -> {
            while (running) {
                Platform.runLater(() -> {
                    double vValue = textScrollPane.getVvalue();
                    vValue += scrollSpeed / 1000.0;
                    if (vValue >= 1.0) {
                        vValue = 0.0;
                    }
                    textScrollPane.setVvalue(vValue);
                });
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
    }

    public void stop() {
        running = false;
        executorService.shutdownNow();
    }
}
