package com.lecturerecorder.services.telepromter;

import com.lecturerecorder.utils.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import javafx.scene.paint.Color;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TeleprompterService {

    public enum Speed {
        SLOW(1), NORMAL(2), FAST(3);

        private int value;

        Speed(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private String text = "";
    private Speed speed = Speed.NORMAL;
    private volatile boolean paused = false;

    private String fontFamily = "Arial";
    private int fontSize = 24;
    private Color fontColor = Color.BLACK;

    private int currentPosition = 0;

    public TeleprompterService() {
    }

    public TeleprompterService(String text) {
        this.text = text;
    }

    private java.awt.Color convertColor(javafx.scene.paint.Color fxColor) {
        int red = (int) Math.round(fxColor.getRed() * 255);
        int green = (int) Math.round(fxColor.getGreen() * 255);
        int blue = (int) Math.round(fxColor.getBlue() * 255);
        int alpha = (int) Math.round(fxColor.getOpacity() * 255);
        return new java.awt.Color(red, green, blue, alpha);
    }

    public boolean isPaused() {
        return paused;
    }

    public void setSpeed(Speed speed) {
        this.speed = speed;
    }

    public void setText(String text) {
        this.text = text;
        this.currentPosition = 0;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public void setFontColor(Color fontColor) {
        this.fontColor = fontColor;
    }

    public Mat overlayTeleprompter(Mat frame) {
        if (paused || text.isEmpty()) {
            return frame;
        }

        BufferedImage teleprompterImage = new BufferedImage(frame.width(), frame.height(), BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = teleprompterImage.createGraphics();

        g2d.setColor(convertColor(fontColor));
        g2d.setFont(new Font(fontFamily, Font.PLAIN, fontSize));

        int y = frame.height() - 50;

        String[] words = text.substring(currentPosition).split(" ");
        StringBuilder line = new StringBuilder();
        int x = 10;

        for (String word : words) {
            String tempLine = line + word + " ";
            int stringWidth = g2d.getFontMetrics().stringWidth(tempLine);
            if (stringWidth < frame.width() - 20) {
                line.append(word).append(" ");
            } else {
                g2d.drawString(line.toString(), x, y);
                y -= g2d.getFontMetrics().getHeight();
                line = new StringBuilder(word + " ");
            }
        }
        g2d.drawString(line.toString(), x, y);

        g2d.dispose();

        Mat teleprompterMat = Utils.bufferedImageToMat(teleprompterImage);

        Core.addWeighted(frame, 1.0, teleprompterMat, 1.0, 0.0, frame);

        currentPosition += speed.getValue();
        if (currentPosition >= text.length()) {
            currentPosition = 0;
        }

        return frame;
    }
}
