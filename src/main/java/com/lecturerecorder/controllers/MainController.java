package com.lecturerecorder.controllers;

import com.lecturerecorder.model.SettingsModel;
import com.lecturerecorder.services.audio.AudioCaptureService;
import com.lecturerecorder.services.presentation.PresentationOverlayService;
import com.lecturerecorder.services.video.VideoCaptureService;
import com.lecturerecorder.services.video.VideoRecordingService;
import com.lecturerecorder.utils.Utils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.*;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.openimaj.video.capture.Device;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class MainController {

    @FXML
    private ImageView mainImageView;

    @FXML
    private ListView<VBox> sourceListView;

    @FXML
    private Button addSourceButton;

    @FXML
    private Button removeSourceButton;

    @FXML
    private TextArea teleprompterTextArea;

    @FXML
    private ChoiceBox<String> fontFamilyChoiceBox;

    @FXML
    private TextField fontSizeField;

    @FXML
    private ColorPicker fontColorPicker;

    @FXML
    private Button startStopRecordingButton;

    private Future<?> videoPreviewTask;

    private List<VideoCaptureService> videoCaptureServices = new ArrayList<>();

    private Map<ImageView, VideoCaptureService> previewMap = new HashMap<>();

    private VideoCaptureService selectedCaptureService;

    private AudioCaptureService audioCaptureService;
    private Thread audioThread;

    private Map<VideoCaptureService, VideoRecordingService> recordingServices = new HashMap<>();

    private Map<Device, Integer> deviceIndexMap = new HashMap<>();

    private ExecutorService executorService = Executors.newCachedThreadPool();

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private boolean isRecording = false;


    private PresentationOverlayService presentationOverlayService;


    private double teleprompterSpeed = 1.0;


    @FXML
    public void initialize() {
        presentationOverlayService = new PresentationOverlayService();
        mainImageView.setPreserveRatio(true);
        startMainVideoPreview();
    }

    private List<String> getAvailableFonts() {
        return javafx.scene.text.Font.getFamilies();
    }

    @FXML
    private void handleAddSource(ActionEvent event) {
        initializeDeviceIndexMap();
        Map<String, Device> deviceNameMap = new LinkedHashMap<>();
        for (Device device : deviceIndexMap.keySet()) {
            if (device != null && device.getNameStr() != null) {
                deviceNameMap.put(device.getNameStr(), device);
            }
        }

        List<String> deviceNames = new ArrayList<>(deviceNameMap.keySet());

        if (deviceNames.isEmpty()) {
            showError("Ошибка", "Не найдено доступных камер");
            return;
        }

        String defaultDeviceName = deviceNames.get(0);

        ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultDeviceName, deviceNames);
        dialog.setTitle("Добавить источник видео");
        dialog.setHeaderText("Выберите камеру из списка");
        dialog.setContentText("Камера:");

        Stage stage = (Stage) mainImageView.getScene().getWindow();
        dialog.initOwner(stage);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(deviceName -> {
            Device device = deviceNameMap.get(deviceName);
            if (device != null) {
                try {
                    FXMLLoader loader = new FXMLLoader();
                    loader.setLocation(getClass().getResource("/fxml/SourceSettings.fxml"));
                    Parent page = loader.load();

                    Stage dialogStage = new Stage();
                    dialogStage.setTitle("Настройки источника");
                    dialogStage.initModality(Modality.WINDOW_MODAL);
                    dialogStage.initOwner(stage);
                    dialogStage.setScene(new Scene(page));

                    SourceSettingsController controller = loader.getController();
                    controller.setDialogStage(dialogStage);

                    int deviceIndex = deviceIndexMap.get(device);
                    VideoCapture videoCapture = new VideoCapture(deviceIndex);
                    if (!videoCapture.isOpened()) {
                        throw new RuntimeException("Не удалось открыть устройство видеозахвата с индексом: " + deviceIndex);
                    }
                    controller.setInitaialValues(videoCapture);
                    videoCapture.release();


                    dialogStage.showAndWait();

                    if (controller.isOkClicked()) {
                        deviceIndex = deviceIndexMap.get(device);
                        int width = controller.getSelectedWidth();
                        int height = controller.getSelectedHeight();
                        int fps = controller.getSelectedFPS();

                        VideoCaptureService captureService = new VideoCaptureService(deviceIndex, width, height, fps);
                        videoCaptureServices.add(captureService);
                        executorService.submit(captureService);

                        addSourcePreview(captureService, device.getNameStr());
                    }
                } catch (Exception e) {
                    showError("Ошибка", "Не удалось добавить источник видео");
                    e.printStackTrace();
                }
            } else {
                showError("Ошибка", "Не удалось найти выбранное устройство");
            }
        });
    }

    @FXML
    private void handleRemoveSource(ActionEvent event) {
        VBox selectedVBox = sourceListView.getSelectionModel().getSelectedItem();
        if (selectedVBox != null) {
            ImageView selectedImageView = (ImageView) selectedVBox.getChildren().get(0);
            VideoCaptureService captureService = previewMap.get(selectedImageView);

            captureService.stop();
            videoCaptureServices.remove(captureService);

            VideoRecordingService recordingService = recordingServices.get(captureService);
            if (recordingService != null) {
                recordingService.stop();
                recordingServices.remove(captureService);
            }

            Platform.runLater(() -> {
                sourceListView.getItems().remove(selectedVBox);
            });

            previewMap.remove(selectedImageView);

            if (selectedCaptureService == captureService) {
                selectedCaptureService = null;
            }
        } else {
            showError("Ошибка", "Пожалуйста, выберите источник для удаления");
        }
    }

    @FXML
    private void handleSlowSpeed(ActionEvent event) {
        teleprompterSpeed = 0.5;
    }

    @FXML
    private void handleNormalSpeed(ActionEvent event) {
        teleprompterSpeed = 1.0;
    }

    @FXML
    private void handleFastSpeed(ActionEvent event) {
        teleprompterSpeed = 2.0;
    }

    @FXML
    private void handleStartTeleprompter(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/TeleprompterView.fxml"));
            Parent root = loader.load();

            TeleprompterWindowController controller = loader.getController();

            controller.setTeleprompterText(teleprompterTextArea.getText());
            controller.setFontFamily(SettingsModel.getInstance().getTeleprompterFontFamily());
            controller.setFontSize(SettingsModel.getInstance().getTeleprompterFontSize()  );
            controller.setFontColor(SettingsModel.getInstance().getTeleprompterFontColor());
            controller.setScrollSpeed(teleprompterSpeed);
            controller.setMainCaptureService(selectedCaptureService);
            Stage stage = new Stage();
            stage.setTitle("Суфлёр");
            stage.setScene(new Scene(root));
            stage.initOwner(mainImageView.getScene().getWindow());
            stage.show();

        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть окно суфлёра");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLoadPresentation(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл презентации");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(mainImageView.getScene().getWindow());
        if (selectedFile != null) {
            try {
                presentationOverlayService.loadPresentation(selectedFile);
            } catch (IOException e) {
                showError("Ошибка", "Не удалось загрузить презентацию: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handlePreviousSlide(ActionEvent event) {
        presentationOverlayService.previousSlide();
    }

    @FXML
    private void handleNextSlide(ActionEvent event) {
        presentationOverlayService.nextSlide();
    }

    @FXML
    private void handleStartStopRecording(ActionEvent event) {
        if (isRecording) {
            stopRecording();
            startStopRecordingButton.setText("Начать запись");
            isRecording = false;
        } else {
            startRecording();
            startStopRecordingButton.setText("Остановить запись");
            isRecording = true;
        }
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/SettingsView.fxml"));
            Parent page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Настройки");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(mainImageView.getScene().getWindow());
            dialogStage.setScene(new Scene(page));

            SettingsController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                updateSettings();
            }
        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть окно настроек");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExit(ActionEvent event) {
        shutdown();
        Platform.exit();
    }

    private void startRecording() {
        String saveLocation = SettingsModel.getInstance().getOutputPath();

        String folderName = "Record_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File recordingFolder = new File(saveLocation, folderName);
        if (!recordingFolder.exists()) {
            recordingFolder.mkdirs();
        }

        for (int i = 0; i < videoCaptureServices.size(); i++) {
            VideoCaptureService captureService = videoCaptureServices.get(i);

            String fileName = "Source_" + (i + 1) + ".avi";
            File outputFile = new File(recordingFolder, fileName);

            VideoRecordingService recordingService = new VideoRecordingService(captureService ,captureService.getFrameQueue(), outputFile.getAbsolutePath());
            recordingServices.put(captureService, recordingService);
            executorService.submit(recordingService);
        }

        File audioFile = new File(recordingFolder, "audio.wav");


        startAudioRecording(audioFile.getAbsolutePath());

        Platform.runLater(() -> {
            addSourceButton.setDisable(true);
            removeSourceButton.setDisable(true);
        });
    }

    private void stopRecording() {
        for (VideoRecordingService recordingService : recordingServices.values()) {
            recordingService.stop();
        }
        recordingServices.clear();

        if (audioCaptureService != null) {
            audioCaptureService.stop();
        }

        Platform.runLater(() -> {
            addSourceButton.setDisable(false);
            removeSourceButton.setDisable(false);
        });
    }

    private void updateSettings() {
        SettingsModel settings = SettingsModel.getInstance();
    }

    private void startMainVideoPreview() {
        videoPreviewTask = executorService.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (selectedCaptureService != null) {

                        Mat frame = selectedCaptureService.getFrameQueue().poll(100, TimeUnit.MILLISECONDS);
                        if (frame != null) {

                            frame = presentationOverlayService.overlaySlide(frame);

                            Image image = Utils.mat2Image(frame);

                            int frameWidth = frame.width();
                            int frameHeight = frame.height();

                            double fitHeight;
                            if (frameHeight > 400) {
                                fitHeight = 400;
                            } else {
                                fitHeight = frameHeight;
                            }

                            double aspectRatio = (double) frameWidth / frameHeight;
                            double fitWidth = fitHeight * aspectRatio;

                            Platform.runLater(() -> {
                                mainImageView.setImage(image);
                                mainImageView.setFitHeight(fitHeight);
                                mainImageView.setFitWidth(fitWidth);
                            });
                        }
                    } else {
                        if (!videoCaptureServices.isEmpty()) {
                            selectedCaptureService = videoCaptureServices.get(0);
                        } else {
                            Platform.runLater(() -> mainImageView.setImage(null));
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public AudioCaptureService startAudioRecording(String outputPath) {
        String selectedSourceName = SettingsModel.getInstance().getAudioSource();
        if (selectedSourceName == null) {
            showError("Ошибка", "Пожалуйста, выберите аудио источник.");
            return null;
        }

        Mixer.Info selectedMixerInfo = getAvailableAudioSources().stream()
                .filter(info -> info.getName().equals(selectedSourceName))
                .findFirst()
                .orElse(null);

        if (selectedMixerInfo == null) {
            showError("Ошибка", "Не удалось найти выбранный аудио источник.");
            return null;
        }

        File outputFile = new File(outputPath);

       AudioCaptureService
        audioCaptureService = new AudioCaptureService(outputFile, selectedMixerInfo);
        audioThread = new Thread(audioCaptureService);
        audioThread.start();
        return audioCaptureService;
    }

    public void stopAudioRecording() {
        if (audioCaptureService != null) {
            audioCaptureService.stop();
            try {
                audioThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            showError("Ошибка", "Запись аудио не была начата.");
        }
    }

    private void addSourcePreview(VideoCaptureService captureService, String deviceName) {
        ImageView imageView = new ImageView();
        imageView.setFitHeight(100);
        imageView.setFitWidth(150);

        Label sourceLabel = new Label(deviceName);
        VBox sourceBox = new VBox();
        sourceBox.getChildren().addAll(imageView, sourceLabel);

        Platform.runLater(() -> {
            sourceListView.getItems().add(sourceBox);
        });

        previewMap.put(imageView, captureService);

        executorService.submit(() -> {
            while (true) {
                try {
                    Mat frame = captureService.getFrameQueue().peek();
                    if (frame != null) {
                        Mat resizedFrame = new Mat();
                        Size targetSize = new Size(imageView.getFitWidth(), imageView.getFitHeight());
                        Imgproc.resize(frame, resizedFrame, targetSize);

                        Image image = Utils.mat2Image(resizedFrame);

                        Platform.runLater(() -> imageView.setImage(image));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        });

        imageView.setOnMouseClicked(event -> {
            selectedCaptureService = captureService;
        });

        if (selectedCaptureService == null) {
            selectedCaptureService = captureService;
        }
    }

    private void initializeDeviceIndexMap() {
        deviceIndexMap.clear();
        List<Device> devices = getAvailableDevices();
        int index = 0;
        for (Device device : devices) {
            if (device != null) {
                VideoCapture tempCapture = new VideoCapture(index);
                if (tempCapture.isOpened()) {
                    deviceIndexMap.put(device, index);
                    tempCapture.release();
                }
            }
            index++;
        }
    }

    private List<Device> getAvailableDevices() {
        return org.openimaj.video.capture.VideoCapture.getVideoDevices();
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

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            Stage stage = (Stage) mainImageView.getScene().getWindow();
            alert.initOwner(stage);
            alert.showAndWait();
        });
    }

    public void shutdown() {
        executorService.shutdownNow();
        for (VideoCaptureService captureService : videoCaptureServices) {
            captureService.stop();
        }
        for (VideoRecordingService recordingService : recordingServices.values()) {
            recordingService.stop();
        }
        if (audioCaptureService != null) {
            audioCaptureService.stop();
        }
        presentationOverlayService.stop();
    }
}
