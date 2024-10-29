package com.lecturerecorder.model;


import javafx.scene.paint.Color;

import java.util.prefs.Preferences;
import javafx.scene.paint.Color;

public class SettingsModel {
    private static SettingsModel instance;
    private Preferences prefs;


    private String outputPath;
    private int teleprompterFontSize;
    private Color teleprompterFontColor;
    private String teleprompterFontFamily;
    private String audioSource;

    private static final String OUTPUT_PATH_KEY = "outputPath";
    private static final String FONT_SIZE_KEY = "teleprompterFontSize";
    private static final String FONT_COLOR_KEY = "teleprompterFontColor";
    private static final String FONT_FAMILY_KEY = "teleprompterFontFamily";
    private static final String AUDIO_SOURCE_KEY = "audioSource";

    private SettingsModel() {
        prefs = Preferences.userNodeForPackage(SettingsModel.class);
        loadSettings();
    }

    public static synchronized SettingsModel getInstance() {
        if (instance == null) {
            instance = new SettingsModel();
        }
        return instance;
    }


    public void loadSettings() {
        outputPath = prefs.get(OUTPUT_PATH_KEY, System.getProperty("user.home"));
        teleprompterFontSize = prefs.getInt(FONT_SIZE_KEY, 24);
        teleprompterFontColor = Color.web(prefs.get(FONT_COLOR_KEY, "#FFFFFF"));
        teleprompterFontFamily = prefs.get(FONT_FAMILY_KEY, "Arial");
        audioSource = prefs.get(AUDIO_SOURCE_KEY, "Default");
    }

    public void saveSettings() {
        prefs.put(OUTPUT_PATH_KEY, outputPath);
        prefs.putInt(FONT_SIZE_KEY, teleprompterFontSize);
        prefs.put(FONT_COLOR_KEY, teleprompterFontColor.toString());
        prefs.put(FONT_FAMILY_KEY, teleprompterFontFamily);
        prefs.put(AUDIO_SOURCE_KEY, audioSource);
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public int getTeleprompterFontSize() {
        return teleprompterFontSize;
    }

    public void setTeleprompterFontSize(int teleprompterFontSize) {
        this.teleprompterFontSize = teleprompterFontSize;
    }

    public Color getTeleprompterFontColor() {
        return teleprompterFontColor;
    }

    public void setTeleprompterFontColor(Color teleprompterFontColor) {
        this.teleprompterFontColor = teleprompterFontColor;
    }

    public String getTeleprompterFontFamily() {
        return teleprompterFontFamily;
    }

    public void setTeleprompterFontFamily(String teleprompterFontFamily) {
        this.teleprompterFontFamily = teleprompterFontFamily;
    }

    public void setAudioSource(String audioSource) {
        this.audioSource = audioSource;
    }

    public String getAudioSource() {
        return audioSource;
    }
}