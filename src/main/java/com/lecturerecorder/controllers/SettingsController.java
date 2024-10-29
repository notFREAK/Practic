package com.lecturerecorder.controllers;

import com.lecturerecorder.services.audio.AudioCaptureService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.paint.Color;

import com.lecturerecorder.model.SettingsModel;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SettingsController {

    @FXML
    private TextField saveLocationField;

    @FXML
    private ChoiceBox<String> audioSourceChoiceBox;

    @FXML
    private TextField fontSizeField;

    @FXML
    private ColorPicker fontColorPicker;

    @FXML
    private ChoiceBox<String> fontFamilyChoiceBox;

    private Stage dialogStage;
    private boolean saveClicked = false;
    private List<Mixer.Info> audioSources;
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void initialize() {
        SettingsModel settings = SettingsModel.getInstance();

        saveLocationField.setText(settings.getOutputPath());
        fontSizeField.setText(String.valueOf(settings.getTeleprompterFontSize()));
        fontColorPicker.setValue(settings.getTeleprompterFontColor());
        fontFamilyChoiceBox.setItems(FXCollections.observableArrayList(getAvailableFonts()));
        fontFamilyChoiceBox.getSelectionModel().select(settings.getTeleprompterFontFamily());

        audioSources = getAvailableAudioSources();
        List<String> sourceNames = audioSources.stream()
                .map(Mixer.Info::getName)
                .collect(Collectors.toList());
        audioSourceChoiceBox.setItems(FXCollections.observableArrayList(sourceNames));

        String currentAudioSource = settings.getAudioSource();
        if (!sourceNames.isEmpty()) {
            audioSourceChoiceBox.getSelectionModel().selectFirst();
        } else {
            showError("Ошибка", "Нет доступных аудио источников.");
        }
    }

    @FXML
    private void handleChooseSaveLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(dialogStage);
        if (selectedDirectory != null) {
            saveLocationField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void handleSave() {
        SettingsModel settings = SettingsModel.getInstance();
        settings.setOutputPath(saveLocationField.getText());
        settings.setTeleprompterFontSize(Integer.parseInt(fontSizeField.getText()));
        settings.setTeleprompterFontColor(fontColorPicker.getValue());
        settings.setTeleprompterFontFamily(fontFamilyChoiceBox.getSelectionModel().getSelectedItem());
        settings.setAudioSource(audioSourceChoiceBox.getSelectionModel().getSelectedItem());

        settings.saveSettings();

        saveClicked = true;
        dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }


    public static List<Mixer.Info> getAvailableAudioSources() {
        List<Mixer.Info> audioSources = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] targetLineInfo = mixer.getTargetLineInfo(new Line.Info(TargetDataLine.class));

            if (targetLineInfo.length > 0) {
                audioSources.add(mixerInfo);
            }
        }
        return audioSources;
    }

    private List<String> getAvailableFonts() {
        return javafx.scene.text.Font.getFamilies();
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