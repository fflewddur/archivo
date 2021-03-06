/*
 * Copyright 2015-2016 Todd Kulesza <todd@dropline.net>.
 *
 * This file is part of Archivo.
 *
 * Archivo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Archivo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Archivo.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.straylightlabs.archivo;

import ch.qos.logback.classic.Level;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.straylightlabs.archivo.controller.*;
import net.straylightlabs.archivo.model.*;
import net.straylightlabs.archivo.net.MindCommandRecordingUpdate;
import net.straylightlabs.archivo.net.MindTask;
import net.straylightlabs.archivo.utilities.OSHelper;
import net.straylightlabs.archivo.view.*;
import org.controlsfx.control.HyperlinkLabel;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Archivo extends Application {
    private Stage primaryStage;
    private final MAKManager maks;
    private final StringProperty statusText;
    private final ExecutorService rpcExecutor;
    private final UserPrefs prefs;
    private RootLayoutController rootController;
    private RecordingListController recordingListController;
    private RecordingDetailsController recordingDetailsController;
    private final ArchiveQueueManager archiveQueueManager;
    private final CrashReportController crashReportController;
    private ArchiveHistory archiveHistory;
    private GlyphFont symbolFont;

    private final static Logger logger = LoggerFactory.getLogger(Archivo.class);
    public final static TelemetryController telemetryController = new TelemetryController();

    public static final String APPLICATION_NAME = "Archivo";
    public static final String APPLICATION_RDN = "net.straylightlabs.archivo";
    public static final int APP_MAJOR_VERSION = 1;
    public static final int APP_MINOR_VERSION = 1;
    public static final int APP_RELEASE_VERSION = 0;
    public static final boolean IS_BETA = false;
    public static final int BETA_VERSION = 0;
    public static final String APPLICATION_VERSION;
    public static final String USER_AGENT;
    public static final Path LOG_PATH = Paths.get(OSHelper.getDataDirectory().toString(), "log.txt");
    private static final int WINDOW_MIN_HEIGHT = 400;
    private static final int WINDOW_MIN_WIDTH = 555;
    private static final Path ARCHIVE_HISTORY_PATH = Paths.get(OSHelper.getDataDirectory().toString(), "history.xml");

    static {
        if (IS_BETA) {
            APPLICATION_VERSION = String.format(
                    "%d.%d.%d Beta %d", APP_MAJOR_VERSION, APP_MINOR_VERSION, APP_RELEASE_VERSION, BETA_VERSION
            );
        } else {
            APPLICATION_VERSION = String.format("%d.%d.%d", APP_MAJOR_VERSION, APP_MINOR_VERSION, APP_RELEASE_VERSION);
        }
        USER_AGENT = String.format("%s/%s", APPLICATION_NAME, APPLICATION_VERSION);
    }

    public Archivo() {
        super();
        prefs = new UserPrefs();
        maks = new MAKManager();
        prefs.loadMAKs(maks);
        if (prefs.getShareTelemetry()) {
            telemetryController.enable();
        }
        setLogLevel();
        telemetryController.setUserId(prefs.getUserId());
        crashReportController = new CrashReportController(prefs.getUserId());
        statusText = new SimpleStringProperty();
        rpcExecutor = Executors.newSingleThreadExecutor();
        archiveQueueManager = new ArchiveQueueManager(this);
    }

    /**
     * If the user has requested debugging mode, set the root logger to the DEBUG level
     */
    private void setLogLevel() {
        if (prefs.getDebugMode() || Archivo.IS_BETA) {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                    ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME
            );
            root.setLevel(Level.DEBUG);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (!prefs.parseParameters(getParameters())) {
            cleanShutdown();
        }

        logger.info("Starting up {} {}...", APPLICATION_NAME, APPLICATION_VERSION);
        logVMInfo();

        loadSymbolFont();
        archiveHistory = ArchiveHistory.loadFrom(ARCHIVE_HISTORY_PATH);

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(APPLICATION_NAME);
        this.primaryStage.setMinHeight(WINDOW_MIN_HEIGHT);
        this.primaryStage.setMinWidth(WINDOW_MIN_WIDTH);
        restoreWindowDimensions();

        initRootLayout();

        String mak = maks.currentMAK();
        if (mak == null) {
            try {
                SetupDialog dialog = new SetupDialog(primaryStage);
                mak = dialog.promptUser();
                maks.addMAK(mak);
                prefs.saveMAKs(maks);
            } catch (IllegalStateException e) {
                logger.error("Error getting MAK from user: ", e);
                cleanShutdown();
            }
        }

        initRecordingList();
        initRecordingDetails();

        archiveQueueManager.addObserver(rootController);

        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            if (confirmTaskCancellation()) {
                archiveQueueManager.cancelAllArchiveTasks();
                cleanShutdown();
            }
        });

        prefs.addNetworkChangedListener(((observable, oldValue, newValue) -> recordingListController.startTivoSearch()));
        telemetryController.sendStartupEvent();
        checkForUpdates();
    }

    private void loadSymbolFont() {
        URL fontUrl = getClass().getClassLoader().getResource("resources/fontawesome.otf");
        logger.debug("Loading font resource at {}", fontUrl);
        if (fontUrl == null) {
            logger.error("Error loading symbol font");
        } else {
            symbolFont = new FontAwesome(fontUrl.toString());
        }
    }

    public GlyphFont getSymbolFont() {
        return symbolFont;
    }

    public Glyph getGlyph(Enum<?> glyphName) {
        if (symbolFont == null) {
            logger.warn("No symbol font available");
            return null;
        }
        return symbolFont.create(glyphName);
    }

    private void logVMInfo() {
        logger.info("Running on Java {} from {}", System.getProperty("java.version"), System.getProperty("java.vendor"));
        logger.info("System is {} (version = {}, arch = {})", System.getProperty("os.name"),
                System.getProperty("os.version"), System.getProperty("os.arch"));
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            try {
                FileStore store = Files.getFileStore(root);
                logger.info("Volume {} has {} MB free of {} MB", root,
                        store.getUsableSpace() / 1024 / 1024, store.getTotalSpace() / 1024 / 1024);
            } catch (FileSystemException e) {
                // Ignore these
            } catch (IOException e) {
                logger.error("Error getting available disk space for volume {}: ", root, e);
            }
        }
    }

    private void checkForUpdates() {
        Task<SoftwareUpdateDetails> updateCheck = new UpdateCheckTask();
        updateCheck.setOnSucceeded(event -> {
            SoftwareUpdateDetails update = updateCheck.getValue();
            if (update.isAvailable()) {
                logger.info("Update check: A newer version of {} is available!", APPLICATION_NAME);
                showUpdateDialog(update);
            } else {
                logger.info("Update check: This is the latest version of {} ({})", APPLICATION_NAME, APPLICATION_VERSION);
            }
        });
        updateCheck.setOnFailed(event -> logger.error("Error checking for updates: ", event.getSource().getException()));
        Executors.newSingleThreadExecutor().submit(updateCheck);
    }

    private void showUpdateDialog(SoftwareUpdateDetails updateDetails) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Update Available");
        alert.setHeaderText(String.format("A new version of %s is available", Archivo.APPLICATION_NAME));
        alert.setContentText(String.format("%s %s was released on %s.\n\nNotable changes include %s.\n\n" +
                        "Would you like to install the update now?\n\n",
                Archivo.APPLICATION_NAME, updateDetails.getVersion(),
                updateDetails.getReleaseDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                updateDetails.getSummary()));

        ButtonType deferButtonType = new ButtonType("No, I'll Update Later", ButtonBar.ButtonData.NO);
        ButtonType updateButtonType = new ButtonType("Yes, Let's Update Now", ButtonBar.ButtonData.YES);

        alert.getButtonTypes().setAll(deferButtonType, updateButtonType);
        ((Button) alert.getDialogPane().lookupButton(deferButtonType)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(updateButtonType)).setDefaultButton(true);

        Optional<ButtonType> result = alert.showAndWait();
        if ((result.isPresent() && result.get() == updateButtonType)) {
            try {
                Desktop.getDesktop().browse(updateDetails.getLocation().toURI());
            } catch (URISyntaxException | IOException e) {
                Archivo.logger.error("Error opening a web browser to download '{}': ", updateDetails.getLocation(), e);
            }
        }
    }

    public void cleanShutdown() {
        if (!confirmTaskCancellation()) {
            return;
        }

        archiveHistory.save();
        saveWindowDimensions();

        int waitTimeMS = 100;
        int msLimit = 15000;
        if (archiveQueueManager.hasTasks()) {
            setStatusText("Exiting...");
            try {
                int msWaited = 0;
                archiveQueueManager.cancelAllArchiveTasks();
                while (archiveQueueManager.hasTasks() && msWaited < msLimit) {
                    Thread.sleep(waitTimeMS);
                    msWaited += waitTimeMS;
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for archive tasks to shutdown: ", e);
            }
        }

        boolean reportingCrashes = crashReportController.hasCrashReport() && getUserPrefs().getShareTelemetry();
        if (reportingCrashes) {
            setStatusText("Exiting...");
            Task<Void> crashReportTask = crashReportController.buildTask();
            crashReportTask.setOnSucceeded(event -> shutdown());
            crashReportTask.setOnFailed(event -> shutdown());
            Executors.newSingleThreadExecutor().submit(crashReportTask);
        } else {
            shutdown();
        }
    }

    private void shutdown() {
        logger.info("Shutting down.");
        Platform.exit();
        System.exit(0);
    }

    /**
     * If there are active tasks, prompt the user before exiting.
     * Returns true if the user wants to cancel all tasks and exit.
     */
    private boolean confirmTaskCancellation() {
        if (archiveQueueManager.hasTasks()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Cancel Task Confirmation");
            alert.setHeaderText("Really cancel all tasks and exit?");
            alert.setContentText("You are currently archiving recordings from your TiVo. Are you sure you want to " +
                    "close Archivo and cancel these tasks?");

            ButtonType cancelButtonType = new ButtonType("Cancel tasks and exit", ButtonBar.ButtonData.NO);
            ButtonType keepButtonType = new ButtonType("Keep archiving", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(cancelButtonType, keepButtonType);
            ((Button) alert.getDialogPane().lookupButton(cancelButtonType)).setDefaultButton(false);
            ((Button) alert.getDialogPane().lookupButton(keepButtonType)).setDefaultButton(true);

            Optional<ButtonType> result = alert.showAndWait();
            if (!result.isPresent()) {
                logger.error("No result from alert dialog");
                return false;
            } else {
                return (result.get() == cancelButtonType);
            }
        }
        return true;
    }

    private void saveWindowDimensions() {
        if (primaryStage.isMaximized()) {
            getUserPrefs().setWindowMaximized(true);
        } else {
            getUserPrefs().setWindowMaximized(false);
            getUserPrefs().setWindowWidth((int) primaryStage.getWidth());
            getUserPrefs().setWindowHeight((int) primaryStage.getHeight());
        }
    }

    private void restoreWindowDimensions() {
        Archivo.logger.debug("Restoring window width of {}", getUserPrefs().getWindowWidth());
        Archivo.logger.debug("Restoring window height of {}", getUserPrefs().getWindowHeight());
        Archivo.logger.debug("Restoring window maximized: {}", getUserPrefs().isWindowMaximized());
        primaryStage.setWidth(getUserPrefs().getWindowWidth());
        primaryStage.setHeight(getUserPrefs().getWindowHeight());
        primaryStage.setMaximized(getUserPrefs().isWindowMaximized());
    }

    private void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Archivo.class.getResource("view/RootLayout.fxml"));
            BorderPane rootLayout = loader.load();

            rootController = loader.getController();
            rootController.setMainApp(this);
            rootController.disableMenuItems();

            Scene scene = new Scene(rootLayout);
            URL styleUrl = getClass().getClassLoader().getResource("resources/style.css");
            if (styleUrl != null) {
                scene.getStylesheets().add(styleUrl.toExternalForm());
            }
            primaryStage.setScene(scene);

            primaryStage.getIcons().addAll(
                    new Image(getClass().getClassLoader().getResourceAsStream("resources/archivo-16.png")),
                    new Image(getClass().getClassLoader().getResourceAsStream("resources/archivo-32.png")),
                    new Image(getClass().getClassLoader().getResourceAsStream("resources/archivo-64.png")),
                    new Image(getClass().getClassLoader().getResourceAsStream("resources/archivo-96.png")),
                    new Image(getClass().getClassLoader().getResourceAsStream("resources/archivo-128.png")),
                    new Image(getClass().getClassLoader().getResourceAsStream("resources/archivo-48.png"))
            );
            primaryStage.show();
        } catch (IOException e) {
            logger.error("Error initializing main window: ", e);
        }
    }

    private void initRecordingList() {
        assert (rootController != null);

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Archivo.class.getResource("view/RecordingList.fxml"));

            recordingListController = new RecordingListController(this);
            loader.setController(recordingListController);

            Pane recordingList = loader.load();
            rootController.getMainGrid().add(recordingList, 0, 0);
            rootController.setMenuBindings(recordingListController);

            recordingListController.startTivoSearch();
        } catch (IOException e) {
            logger.error("Error initializing recording list: ", e);
        }
    }

    private void initRecordingDetails() {
        assert (rootController != null);
        assert (recordingListController != null);

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Archivo.class.getResource("view/RecordingDetails.fxml"));

            recordingDetailsController = new RecordingDetailsController(this);
            loader.setController(recordingDetailsController);

            Pane recordingDetails = loader.load();
            rootController.getMainGrid().add(recordingDetails, 0, 1);
        } catch (IOException e) {
            logger.error("Error initializing recording details: ", e);
        }
    }

    public void enqueueRecordingForArchiving(Recording recording) {
        if (!archiveQueueManager.enqueueArchiveTask(recording, getActiveTivo(), getMak())) {
            logger.error("Error adding recording to queue");
        }
    }

    public void cancelArchiving(Recording recording) {
        archiveQueueManager.cancelArchiveTask(recording);
    }

    /**
     * Cancel all of the current and queued Archive tasks
     */
    public void cancelAll() {
        archiveQueueManager.cancelAllArchiveTasks();
    }

    public void archiveSelection() {
        List<Recording> toArchive = new ArrayList<>();
        for (Recording recording : recordingListController.getRecordingSelection().getRecordingsWithChildren()) {
            if (recording.isSeriesHeading()) {
                continue;
            }
            boolean archive;
            if (getUserPrefs().getOrganizeArchivedShows()) {
                archive = setOrganizedPath(recording);
            } else {
                SaveFileDialog dialog = new SaveFileDialog(getPrimaryStage(), recording, prefs);
                archive = dialog.showAndWait();
            }
            if (!archive) {
                break;
            }
            logger.info("Archive recording {} to {} (file type = {})...",
                    recording.getFullTitle(), recording.getDestination(), recording.getDestinationType()
            );
            toArchive.add(recording);
        }
        if (getUserPrefs().getOrganizeArchivedShows()) {
            List<Recording> destExists = toArchive.stream().filter(recording -> Files.exists(recording.getDestination())).collect(Collectors.toList());
            destExists.addAll(RecordingsExistDialog.getRecordingsWithDuplicateDestination(toArchive));
            if (destExists.size() > 0) {
                RecordingsExistDialog recordingsExistDialog = new RecordingsExistDialog(this, toArchive, destExists, prefs);
                if (!recordingsExistDialog.showAndWait()) {
                    // The the user canceled the dialog, don't archive anything
                    return;
                }
            }
        }
        // Enqueue selected recordings for archiving
        for (Recording recording : toArchive) {
            recording.setStatus(ArchiveStatus.QUEUED);
            enqueueRecordingForArchiving(recording);
        }
    }



    private boolean setOrganizedPath(Recording recording) {
        Path path = null;
        try {
            FileType fileType = FileType.fromDescription(getUserPrefs().getMostRecentFileType());
            path = Paths.get(
                    getLastFolder().toString(),
                    recording.getDefaultNestedPath().toString() + fileType.getExtension().substring(1)
            );
            Files.createDirectories(path.getParent());
            recording.setDestinationType(fileType);
            recording.setDestination(path);
            return true;
        } catch (IOException e) {
            logger.error("Error creating directory '{}': ", path.getParent(), e);
        }
        return false;
    }

    public void deleteFromTivo(List<Recording> recordings) {
        for (Recording recording : recordings) {
            Archivo.logger.info("User requested we delete {}", recording.getFullTitle());
            Tivo tivo = getActiveTivo();
            if (tivo != null) {
                if (confirmDelete(recording, tivo)) {
                    sendDeleteCommand(recording, tivo);
                }
            } else {
                Archivo.logger.error("No TiVo is selected to delete from");
            }
        }
    }

    private boolean confirmDelete(Recording recording, Tivo tivo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Recording Confirmation");
        alert.setHeaderText("Really remove this recording?");
        alert.setContentText(String.format("Are you sure you want to delete %s from %s?",
                recording.getFullTitle(), tivo.getName()));

        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.NO);
        ButtonType keepButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(deleteButtonType, keepButtonType);
        ((Button) alert.getDialogPane().lookupButton(deleteButtonType)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(keepButtonType)).setDefaultButton(true);

        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent()) {
            logger.error("No result from alert dialog");
            return false;
        } else {
            return (result.get() == deleteButtonType);
        }
    }

    private void sendDeleteCommand(Recording recording, Tivo tivo) {
        assert (recording != null);
        assert (tivo != null);

        setStatusText(String.format("Deleting '%s' from %s...", recording.getTitle(), tivo.getName()));
        primaryStage.getScene().setCursor(Cursor.WAIT);
        MindCommandRecordingUpdate command = new MindCommandRecordingUpdate(recording.getRecordingId(), tivo.getBodyId());
        MindTask task = new MindTask(tivo.getClient(), command);
        task.setOnSucceeded(event -> {
            recordingListController.updateTivoDetails(tivo);
            recordingListController.removeRecording(recording);
            primaryStage.getScene().setCursor(Cursor.DEFAULT);
        });
        task.setOnFailed(event -> {
            Throwable e = event.getSource().getException();
            Archivo.logger.error("Error fetching recordings from {}: ", tivo.getName(), e);
            clearStatusText();
            primaryStage.getScene().setCursor(Cursor.DEFAULT);
            showErrorMessage("Problem deleting recording",
                    String.format("Unfortunately we encountered a problem while removing '%s' from %s. " +
                                    "This usually means that either your computer or your TiVo has lost " +
                                    "its network connection.%n%nError message: %s",
                            recording.getTitle(), tivo.getName(), e.getLocalizedMessage())
            );
        });
        rpcExecutor.submit(task);
    }

    public void expandShows() {
        recordingListController.expandShows();
    }

    public void collapseShows() {
        recordingListController.collapseShows();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private Tivo getActiveTivo() {
        return recordingListController.getSelectedTivo();
    }

    public ExecutorService getRpcExecutor() {
        return rpcExecutor;
    }

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public void setStatusText(String status) {
        logger.info("Setting status to '{}'", status);
        statusText.set(status);
        rootController.showStatus();
    }

    public void clearStatusText() {
        logger.info("TaskStatus cleared");
        rootController.hideStatus();
    }

    public void showPreferencesDialog() {
        PreferencesDialog preferences = new PreferencesDialog(getPrimaryStage(), this);
        preferences.show();
    }

    public void showErrorMessage(String header, String message) {
        showErrorMessageWithAction(header, message, null, null);
    }

    public boolean showErrorMessageWithAction(String header, String message, String action) {
        return showErrorMessageWithAction(header, message, action, null);
    }

    public boolean showErrorMessageWithAction(String header, String message, String action,
                                              EventHandler<ActionEvent> eventHandler) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Something went wrong...");
        alert.setHeaderText(header);

        VBox contentPane = new VBox();
        HyperlinkLabel contentText = new HyperlinkLabel(message);
        contentText.setPrefWidth(500);
        contentPane.setPadding(new Insets(30, 15, 20, 15));
        contentPane.getChildren().add(contentText);
        alert.getDialogPane().setContent(contentPane);

        if (eventHandler != null) {
            contentText.setOnAction(eventHandler);
            contentText.addEventHandler(ActionEvent.ACTION, (event) -> alert.close());
        }

        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType actionButtonType = new ButtonType(action, ButtonBar.ButtonData.YES);
        ButtonType reportButtonType = new ButtonType("Report Problem", ButtonBar.ButtonData.HELP_2);
        alert.getButtonTypes().setAll(closeButtonType, reportButtonType);
        if (action != null) {
            alert.getButtonTypes().add(actionButtonType);
            ((Button) alert.getDialogPane().lookupButton(closeButtonType)).setDefaultButton(false);
            ((Button) alert.getDialogPane().lookupButton(reportButtonType)).setDefaultButton(false);
            ((Button) alert.getDialogPane().lookupButton(actionButtonType)).setDefaultButton(true);
        }

        Optional<ButtonType> result = alert.showAndWait();
        while (result.isPresent() && result.get() == reportButtonType) {
            rootController.reportProblem(null);
            result = alert.showAndWait();
        }
        return (result.isPresent() && result.get() == actionButtonType);
    }

    public void tryNextMAK() {
        String nextMak = maks.tryNextMAK();
        if (nextMak == null) {
            promptForMAK();
        } else {
            updateMAK(nextMak);
        }
    }

    private void promptForMAK() {
        ChangeMAKDialog dialog = new ChangeMAKDialog(primaryStage, maks.currentMAK());
        try {
            updateMAK(dialog.promptUser());
        } catch (IllegalStateException e) {
            cleanShutdown();
        }
    }

    private void updateMAK(String newMak) {
        if (newMak == null || newMak.isEmpty()) {
            logger.error("MAK cannot be empty");
        } else {
            maks.addMAK(newMak);
            prefs.saveMAKs(maks);
            recordingListController.updateMak(newMak);
        }
    }

    public RecordingDetailsController getRecordingDetailsController() {
        return recordingDetailsController;
    }

    @SuppressWarnings("unused")
    public RecordingListController getRecordingListController() {
        return recordingListController;
    }

    public ArchiveHistory getArchiveHistory() {
        return archiveHistory;
    }

    public ArchiveQueueManager getArchiveQueueManager() {
        return archiveQueueManager;
    }

    public UserPrefs getUserPrefs() {
        return prefs;
    }

    public String getMak() {
        return maks.currentMAK();
    }

    public void setLastDevice(Tivo tivo) {
        prefs.setLastDevice(tivo);
    }

    public Tivo getLastDevice() {
        return prefs.getLastDevice(maks.currentMAK());
    }

    public Path getLastFolder() {
        return prefs.getLastFolder();
    }

    public void crashOccurred() {
        crashReportController.crashOccurred();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
