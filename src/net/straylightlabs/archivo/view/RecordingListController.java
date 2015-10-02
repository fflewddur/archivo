/*
 * Copyright 2015 Todd Kulesza <todd@dropline.net>.
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

package net.straylightlabs.archivo.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.Series;
import net.straylightlabs.archivo.model.Tivo;
import net.straylightlabs.archivo.net.MindCommandBodyConfigSearch;
import net.straylightlabs.archivo.net.MindCommandRecordingFolderItemSearch;
import net.straylightlabs.archivo.net.MindTask;
import net.straylightlabs.archivo.net.TivoSearchTask;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class RecordingListController implements Initializable, Observer {
    private final ObservableList<Tivo> tivos;

    private TivoSearchTask tivoSearchTask;
    private boolean alreadyDefaultSorted;

    private ProgressIndicator tablePlaceholderProgressIndicator;
    private Label tablePlaceholderMessage;

    @FXML
    private HBox toolbar;
    @FXML
    private ComboBox<Tivo> tivoList;
    @FXML
    private Button refreshTivoList;
    @FXML
    private TreeTableView<Recording> recordingTreeTable;
    @FXML
    private TreeTableColumn<Recording, String> showColumn;
    @FXML
    private TreeTableColumn<Recording, LocalDateTime> dateColumn;
    @FXML
    private TreeTableColumn<Recording, ArchiveStatus> statusColumn;
    @FXML
    private ProgressBar storageIndicator;
    @FXML
    private Label storageLabel;

    private Archivo mainApp;

    public RecordingListController(Archivo mainApp, List<Tivo> initialTivos) {
        alreadyDefaultSorted = false;
        this.mainApp = mainApp;
        tivos = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(initialTivos));
        tablePlaceholderMessage = new Label("No recordings are available");
        tablePlaceholderProgressIndicator = new ProgressIndicator();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        recordingTreeTable.setShowRoot(false);
        recordingTreeTable.setPlaceholder(tablePlaceholderMessage);

        showColumn.setCellValueFactory(data -> data.getValue().getValue().titleProperty());
        dateColumn.setCellValueFactory(data -> data.getValue().getValue().dateRecordedProperty());
        dateColumn.setCellFactory(col -> new RecordedOnCellFactory());
        dateColumn.setSortType(TreeTableColumn.SortType.DESCENDING);
        dateColumn.setSortable(true);
        statusColumn.setCellValueFactory(data -> data.getValue().getValue().statusProperty());
        statusColumn.setCellFactory(col -> new StatusCellFactory());

        setupContextMenuRowFactory();

        tivoList.setConverter(new Tivo.StringConverter());
        tivoList.getSelectionModel().selectedItemProperty().addListener(
                (tivoList, oldTivo, curTivo) -> {
                    if (curTivo != null) {
                        mainApp.setLastDevice(curTivo);
                        fetchRecordingsFrom(curTivo);
                    }
                }
        );
        tivoList.setItems(tivos);

        // When the list of TiVos is first populated, automatically select one
        tivos.addListener(new TivoListChangeListener());

        Tivo lastDevice = mainApp.getLastDevice();
        if (lastDevice != null && tivos.contains(lastDevice)) {
            tivoList.setValue(lastDevice);
        }

        setupStyles();
    }

    private void setupContextMenuRowFactory() {
        recordingTreeTable.setRowFactory((table) -> {
            final TreeTableRow<Recording> row = new TreeTableRow<>();
            final ContextMenu menu = new ContextMenu();
            MenuItem archive = new MenuItem("Archive...");
            archive.setOnAction(event -> mainApp.getRecordingDetailsController().archive(event));
            MenuItem cancel = new MenuItem("Cancel");
            cancel.setOnAction(event -> mainApp.getRecordingDetailsController().cancel(event));
            MenuItem play = new MenuItem("Play");
            play.setOnAction(event -> mainApp.getRecordingDetailsController().play(event));
            MenuItem delete = new MenuItem("Remove from TiVo...");

            delete.setOnAction(event -> mainApp.deleteFromTivo(table.getSelectionModel().getSelectedItem().getValue()));
            menu.getItems().addAll(archive, cancel, play, new SeparatorMenuItem(), delete);

            row.itemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    archive.disableProperty().bind(newValue.isArchivableProperty().not());
                    cancel.disableProperty().bind(newValue.isCancellableProperty().not());
                    play.disableProperty().bind(newValue.isPlayableProperty().not());
                    delete.disableProperty().bind(newValue.isRemovableProperty().not());
                }
            });

            row.contextMenuProperty().bind(
                    Bindings.when(Bindings.isNotNull(row.itemProperty()))
                            .then(menu)
                            .otherwise((ContextMenu) null)
            );

            return row;
        });
    }

    private void setupStyles() {
        recordingTreeTable.getStyleClass().add("recording-list");
        tablePlaceholderMessage.getStyleClass().add("placeholder-message");
        tablePlaceholderProgressIndicator.setMaxWidth(180);
        tablePlaceholderProgressIndicator.setMaxHeight(180);
    }

    public Tivo getSelectedTivo() {
        return tivoList.getSelectionModel().getSelectedItem();
    }

    @SuppressWarnings("unused")
    public void fetchRecordingsFromSelectedTivo() {
        fetchRecordingsFrom(tivoList.getValue());
    }

    private void fetchRecordingsFrom(Tivo tivo) {
        mainApp.setStatusText("Fetching recordings...");
        recordingTreeTable.setPlaceholder(tablePlaceholderProgressIndicator);
        recordingTreeTable.getSelectionModel().clearSelection();
        disableUI();

        MindCommandRecordingFolderItemSearch command = new MindCommandRecordingFolderItemSearch(tivo);
        MindTask task = new MindTask(tivo.getClient(), command);
        task.setOnSucceeded(event -> {
            fillTreeTableView(command.getSeries());
            updateTivoDetails(tivo);
        });
        task.setOnFailed(event -> {
            Throwable e = event.getSource().getException();
            Archivo.logger.error("Error fetching recordings from {}: ", tivo.getName(), e);
            mainApp.clearStatusText();
            mainApp.showErrorMessage("Problem fetching list of recordings",
                    String.format("Unfortunately we encountered a problem while fetching the list of available " +
                            "recordings from %s. This usually means that either your computer or your TiVo has lost " +
                            "its network connection.%n%nError message: %s", tivo.getName(), e.getLocalizedMessage())
            );

            enableUI();
        });
        mainApp.getRpcExecutor().submit(task);
    }

    private void fillTreeTableView(List<Series> series) {
        List<TreeTableColumn<Recording, ?>> oldSortOrder = recordingTreeTable.getSortOrder().stream().collect(Collectors.toList());
        TreeItem<Recording> root = new TreeItem<>(new Recording.Builder().seriesTitle("root").build());
        TreeItem<Recording> suggestions = new TreeItem<>(new Recording.Builder().seriesTitle("TiVo Suggestions")
                .isSeriesHeading(true).build());
        suggestions.expandedProperty().addListener(new HeaderExpandedHandler(suggestions));

        for (Series s : series) {
            List<Recording> recordings = s.getEpisodes();
            TreeItem<Recording> item;
            boolean allAreSuggestions = true;
            if (recordings.size() > 1) {
                // Create a new tree node with children
                item = new TreeItem<>(new Recording.Builder().seriesTitle(s.getTitle())
                        .numEpisodes(s.getEpisodes().size()).recordedOn(recordings.get(0).getDateRecorded())
                        .isSeriesHeading(true).image(recordings.get(0).getImageURL()).build());
                for (Recording recording : s.getEpisodes()) {
                    allAreSuggestions &= recording.isSuggestion();
                    recording.isChildRecording(true);
                    item.getChildren().add(new TreeItem<>(recording));
                }
                item.setExpanded(true);
            } else {
                // Don't create children for this node
                allAreSuggestions = recordings.get(0).isSuggestion();
                item = new TreeItem<>(recordings.get(0));
            }

            if (allAreSuggestions) {
                // If all of the recordings are TiVo Suggestions, put them under the Suggestions node
                suggestions.getChildren().add(item);
            } else {
                root.getChildren().add(item);
            }
        }
        root.getChildren().add(suggestions);
        recordingTreeTable.setRoot(root);

        Platform.runLater(() -> {
            // Restore the prior sort order
            ObservableList<TreeTableColumn<Recording, ?>> sortOrder = recordingTreeTable.getSortOrder();
            sortOrder.clear();
            if (alreadyDefaultSorted) {
                sortOrder.addAll(oldSortOrder);
            } else {
                sortOrder.add(dateColumn);
                alreadyDefaultSorted = true;
            }
            recordingTreeTable.getSelectionModel().selectFirst();
            recordingTreeTable.requestFocus();
        });
    }

    public void updateTivoDetails(Tivo tivo) {
        MindCommandBodyConfigSearch bodyConfigSearch = new MindCommandBodyConfigSearch(tivo);
        MindTask bodyConfigTask = new MindTask(tivo.getClient(), bodyConfigSearch);
        bodyConfigTask.setOnSucceeded(event -> {
            updateStorageControls(tivo);
            mainApp.clearStatusText();
            enableUI();
        });
        bodyConfigTask.setOnFailed(event -> {
            Throwable e = event.getSource().getException();
            Archivo.logger.error("Error fetching details of {}: ", tivo.getName(), e);
            mainApp.clearStatusText();
            enableUI();
        });
        mainApp.getRpcExecutor().submit(bodyConfigTask);
    }

    /**
     * Setup the "Space used" indicator to reflect the current state of @tivo.
     */
    private void updateStorageControls(Tivo tivo) {
        double percent = (double) tivo.getStorageBytesUsed() / tivo.getStorageBytesTotal();
        int gbUsed = (int) (tivo.getStorageBytesUsed() / (1024 * 1024));
        int gbTotal = (int) (tivo.getStorageBytesTotal() / (1024 * 1024));
        Tooltip storageTooltip = new Tooltip(String.format("%s is %d%% full (%,dGB of %,dGB)",
                tivo.getName(), (int) (percent * 100), gbUsed, gbTotal));
        storageIndicator.setProgress(percent);
        storageIndicator.setTooltip(storageTooltip);
        storageLabel.setTooltip(storageTooltip);
    }

    public void startTivoSearch() {
        assert (mainApp != null);

        if (tivoSearchTask == null) {
            tivoSearchTask = new TivoSearchTask(tivos, mainApp.getMak());
        }

        if (tivos.size() == 0) {
            mainApp.setStatusText("Looking for TiVos...");
            disableUI();
        }

        mainApp.getRpcExecutor().submit(tivoSearchTask);
    }

    public void updateMak(String newMak) {
        restartTivoSearch();
        tivos.stream().forEach(tivo -> tivo.updateMak(newMak));
    }

    public void restartTivoSearch() {
        assert (tivoSearchTask != null);

        tivoSearchTask.cancel();
        startTivoSearch();
    }

    /**
     * Disable the TiVo controls and the recording list
     */
    private void disableUI() {
        mainApp.getPrimaryStage().getScene().setCursor(Cursor.WAIT);
        setUIDisabled(true);
    }

    /**
     * Enable the TiVo controls and the recording list
     */
    private void enableUI() {
        recordingTreeTable.setPlaceholder(tablePlaceholderMessage);
        setUIDisabled(false);
        mainApp.getPrimaryStage().getScene().setCursor(Cursor.DEFAULT);
    }

    private void setUIDisabled(boolean disabled) {
        toolbar.setDisable(disabled);
        recordingTreeTable.setDisable(disabled);
    }

    /**
     * Disable the TiVo controls
     */
    public void disableTivoControls() {
        setTivoControlsDisabled(true);
    }

    /**
     * Enable the TiVo controls
     */
    public void enableTivoControls() {
        setTivoControlsDisabled(false);
    }

    private void setTivoControlsDisabled(boolean disabled) {
        tivoList.setDisable(disabled);
        refreshTivoList.setDisable(disabled);
    }

    public void addRecordingChangedListener(ChangeListener<Recording> listener) {
        // Update our observable property when the selected recording changes
        recordingTreeTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    ObservableValue<Recording> observableRecording = null;
                    Recording oldRecording = null;
                    Recording newRecording = null;
                    if (oldValue != null) {
                        oldRecording = oldValue.getValue();
                    }
                    if (newValue != null) {
                        newRecording = newValue.getValue();
                    }
                    if (observable != null && observable.getValue() != null) {
                        observableRecording = observable.getValue().valueProperty();
                    }
                    listener.changed(observableRecording, oldRecording, newRecording);
                });
    }

    /**
     * Remove @recording for the recording list's data model.
     */
    public void removeRecording(Recording recording) {
        ObservableList<TreeItem<Recording>> recordingItems = recordingTreeTable.getRoot().getChildren();
        removeRecordingFromList(recording, recordingItems);
    }

    /**
     * Recursively search our tree's data model for the recording to delete.
     */
    private void removeRecordingFromList(Recording recording, ObservableList<TreeItem<Recording>> list) {
        Iterator<TreeItem<Recording>> i = list.iterator();
        while (i.hasNext()) {
            TreeItem<Recording> item = i.next();
            if (!item.isLeaf()) {
                removeRecordingFromList(recording, item.getChildren());
            } else {
                Recording other = item.getValue();
                if (other.equals(recording)) {
                    i.remove();
                    return;
                }
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        Boolean hasTasks = (Boolean) arg;
        if (hasTasks) {
            disableTivoControls();
        } else {
            enableTivoControls();
        }
    }

    /**
     * Listen for changes to the list of available TiVo devices
     */
    private class TivoListChangeListener implements ListChangeListener<Tivo> {
        @Override
        public void onChanged(Change<? extends Tivo> c) {
            while (c.next()) {
                if (c.wasAdded()) {
                    // If a new Tivo was found, and we don't currently have a selected Tivo, select the new device
                    if (tivoList.getValue() == null) {
                        tivoList.getSelectionModel().selectFirst();
                        enableUI();
                    }
                }
            }
            // Save our list of known TiVos
            mainApp.setKnownDevices(tivos);
        }
    }

    /**
     * Ensure that when a header is expanded, it scrolls to the top of the view
     */
    private class HeaderExpandedHandler implements ChangeListener<Boolean> {
        private final TreeItem<Recording> item;

        public HeaderExpandedHandler(TreeItem<Recording> item) {
            this.item = item;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            recordingTreeTable.scrollTo(recordingTreeTable.getRow(item));
        }
    }
}
