package com.lecturerecorder.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Utils {

    public static Image mat2Image(Mat frame) {
        try {
            return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BufferedImage matToBufferedImage(Mat original) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", original, mob);
        byte[] byteArray = mob.toArray();
        BufferedImage bufImage = null;
        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bufImage;
    }

    public static Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        int[] data = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
        mat.put(0, 0, intArrayToByteArray(data));
        return mat;
    }

    private static byte[] intArrayToByteArray(int[] intArray) {
        byte[] byteArray = new byte[intArray.length * 3];
        for (int i = 0; i < intArray.length; i++) {
            byteArray[i * 3] = (byte) ((intArray[i]));
            byteArray[i * 3 + 1] = (byte) ((intArray[i]) >> 8);
            byteArray[i * 3 + 2] = (byte) ((intArray[i]) >> 16);
        }
        return byteArray;
    }

    public static Mat MBFImageToMat(MBFImage mbfImage) {
        BufferedImage bufferedImage = ImageUtilities.createBufferedImage(mbfImage);
        return bufferedImageToMat(bufferedImage);
    }
}