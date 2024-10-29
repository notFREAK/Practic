package com.lecturerecorder.services.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioCaptureService implements Runnable {

    private File outputFile;
    private AudioFormat format;
    private TargetDataLine microphone;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    private volatile boolean running = true;

    public AudioCaptureService(File outputFile, Mixer.Info mixerInfo) {
        this.outputFile = outputFile;
        format = new AudioFormat(44100, 16, 2, true, true);
        try {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) mixer.getLine(dataLineInfo);
            microphone.open(format);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            throw new RuntimeException("Не удалось открыть выбранный аудио источник");
        }
    }

    @Override
    public void run() {
        microphone.start();

        try (AudioInputStream audioStream = new AudioInputStream(microphone)) {
            AudioSystem.write(audioStream, fileType, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            microphone.stop();
            microphone.close();
        }
    }

    public void stop() {
        running = false;
        microphone.stop();
        microphone.close();
    }
}
