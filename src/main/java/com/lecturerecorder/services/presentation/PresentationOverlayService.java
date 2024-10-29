package com.lecturerecorder.services.presentation;

import com.lecturerecorder.utils.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;

import javax.imageio.ImageIO;

public class PresentationOverlayService {

    private List<Mat> slides = new ArrayList<>();
    private int currentSlideIndex = 0;

    public PresentationOverlayService() {
    }

    public void loadPresentation(File file) throws IOException {
        slides.clear();
        currentSlideIndex = 0;
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            loadPdfPresentation(file);
        } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            loadImagePresentation(file);
        } else {
            throw new IOException("Неподдерживаемый формат файла");
        }
    }

    private void loadPdfPresentation(File file) throws IOException {
        PDDocument document = PDDocument.load(file);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
            slides.add(Utils.bufferedImageToMat(bim));
        }
        document.close();
    }

    private void loadImagePresentation(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        slides.add(Utils.bufferedImageToMat(image));
    }

    public void nextSlide() {
        if (currentSlideIndex < slides.size() - 1) {
            currentSlideIndex++;
        }
    }

    public void previousSlide() {
        if (currentSlideIndex > 0) {
            currentSlideIndex--;
        }
    }

    public Mat overlaySlide(Mat frame) {
        if (slides.isEmpty()) {
            return frame;
        }
        Mat slide = slides.get(currentSlideIndex);

        Mat resizedSlide = new Mat();
        Imgproc.resize(slide, resizedSlide, new Size(frame.width(), frame.height()));

        Core.addWeighted(frame, 0.7, resizedSlide, 0.3, 0.0, frame);

        return frame;
    }

    public void stop() {
        slides.clear();
    }
}
