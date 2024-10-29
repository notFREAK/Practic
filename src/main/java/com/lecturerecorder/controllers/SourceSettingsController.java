package com.lecturerecorder.controllers;

import com.lecturerecorder.services.video.VideoCaptureService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class SourceSettingsController {

    @FXML
    private TextField widthField;

    @FXML
    private TextField heightField;

    @FXML
    private TextField fpsField;

    private Stage dialogStage;
    private boolean okClicked = false;

    private int selectedWidth;
    private int selectedHeight;
    private int selectedFPS;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    public int getSelectedWidth() {
        return selectedWidth;
    }

    public int getSelectedHeight() {
        return selectedHeight;
    }

    public int getSelectedFPS() {
        return selectedFPS;
    }

    public void setInitaialValues(VideoCapture videoCapture) {
        if (videoCapture != null) {
            this.selectedWidth = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH);
            this.selectedHeight = (int)videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
            this.selectedFPS = (int)videoCapture.get(Videoio.CAP_PROP_FPS);

            widthField.setText(String.valueOf(selectedWidth));
            heightField.setText(String.valueOf(selectedHeight));
            fpsField.setText(String.valueOf(selectedFPS));
        }
    }

    @FXML
    private void handleOk(ActionEvent event) {
        if (isInputValid()) {
            selectedWidth = Integer.parseInt(widthField.getText());
            selectedHeight = Integer.parseInt(heightField.getText());
            selectedFPS = Integer.parseInt(fpsField.getText());
            okClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (widthField.getText() == null || widthField.getText().isEmpty()) {
            errorMessage += "Нет корректной ширины!\n";
        } else {
            try {
                int width = Integer.parseInt(widthField.getText());
                if (width <= 0) {
                    errorMessage += "Ширина должна быть положительным числом!\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "Ширина должна быть числом!\n";
            }
        }

        if (heightField.getText() == null || heightField.getText().isEmpty()) {
            errorMessage += "Нет корректной высоты!\n";
        } else {
            try {
                int height = Integer.parseInt(heightField.getText());
                if (height <= 0) {
                    errorMessage += "Высота должна быть положительным числом!\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "Высота должна быть числом!\n";
            }
        }

        if (fpsField.getText() == null || fpsField.getText().isEmpty()) {
            errorMessage += "Нет корректного FPS!\n";
        } else {
            try {
                int fps = Integer.parseInt(fpsField.getText());
                if (fps <= 0) {
                    errorMessage += "FPS должен быть положительным числом!\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "FPS должен быть числом!\n";
            }
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Некорректный ввод");
            alert.setHeaderText("Пожалуйста, исправьте некорректные поля");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}