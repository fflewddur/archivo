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

package net.straylightlabs.archivo.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.controller.ArchiveQueueManager;
import net.straylightlabs.archivo.model.*;
import net.straylightlabs.archivo.net.MindCommandBodyConfigSearch;
import net.straylightlabs.archivo.net.MindCommandRecordingFolderItemSearch;
import net.straylightlabs.archivo.net.MindTask;
import net.straylightlabs.archivo.net.TivoSearchTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RecordingListController implements Initializable {
    private final ObservableList<Tivo> tivos;
    private TreeItem<Recording> suggestions;
    private final ChangeListener<? super Tivo> tivoSelectedListener;

    private TivoSearchTask tivoSearchTask;
    private boolean alreadyDefaultSorted;
    private boolean uiDisabled;
    private boolean trySearchAgain;

    private final BooleanProperty tivoIsBusy; // set to true when we're communicating w/ the selected device

    private final Label tablePlaceholderMessage;

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

    private final Archivo mainApp;

    private final static Logger logger = LoggerFactory.getLogger(RecordingListController.class);

    public RecordingListController(Archivo mainApp) {
        tivoIsBusy = new SimpleBooleanProperty(false);
        alreadyDefaultSorted = false;
        this.mainApp = mainApp;
        tivos = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        tablePlaceholderMessage = new Label("No recordings are available");
        tivoSelectedListener = (tivoList, oldTivo, curTivo) -> {
            logger.info("New TiVo selected: {}", curTivo);
            if (curTivo != null) {
                mainApp.setLastDevice(curTivo);
                fetchRecordingsFrom(curTivo);
            }
        };
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        recordingTreeTable.setShowRoot(false);
        recordingTreeTable.setPlaceholder(tablePlaceholderMessage);
        recordingTreeTable.setOnSort(event ->
                updateGroupStatus(recordingTreeTable.getRoot(), recordingTreeTable.getRoot().getChildren())
        );
        showColumn.setCellValueFactory(data -> data.getValue().getValue().titleProperty());
        dateColumn.setCellValueFactory(data -> data.getValue().getValue().dateRecordedProperty());
        dateColumn.setCellFactory(col -> new RecordedOnCellFactory());
        dateColumn.setSortType(TreeTableColumn.SortType.DESCENDING);
        statusColumn.setCellValueFactory(data -> data.getValue().getValue().statusProperty());
        statusColumn.setCellFactory(col -> new StatusCellFactory());

        setupContextMenuRowFactory();

        tivoList.setConverter(new Tivo.StringConverter());
        tivoList.setItems(tivos);

        // Disable the TiVo controls when no devices are available
        refreshTivoList.disableProperty().bind(Bindings.or(Bindings.size(tivos).lessThan(1), tivoIsBusy));
        tivoList.disableProperty().bind(Bindings.or(Bindings.size(tivos).lessThan(1), tivoIsBusy));
        storageIndicator.disableProperty().bind(Bindings.or(Bindings.size(tivos).lessThan(1), tivoIsBusy));
        storageLabel.disableProperty().bind(Bindings.or(Bindings.size(tivos).lessThan(1), tivoIsBusy));
        recordingTreeTable.disableProperty().bind(Bindings.or(Bindings.size(tivos).lessThan(1), tivoIsBusy));

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
    }

    public Tivo getSelectedTivo() {
        return tivoList.getSelectionModel().getSelectedItem();
    }

    public ObservableList<Tivo> getTivos() {
        return tivos;
    }

    @SuppressWarnings("unused")
    public void fetchRecordingsFromSelectedTivo() {
        fetchRecordingsFrom(tivoList.getValue());
    }

    private void fetchRecordingsFrom(Tivo tivo) {
        logger.debug("Fetching recordings from {}", tivo);
        mainApp.setStatusText("Fetching recordings...");
        tivoIsBusy.setValue(true);
        recordingTreeTable.getSelectionModel().clearSelection();
        disableUI();

        MindCommandRecordingFolderItemSearch command = new MindCommandRecordingFolderItemSearch(tivo);
        MindTask task = new MindTask(tivo.getClient(), command);
        task.setOnSucceeded(event -> {
            logger.info("Fetching list of recordings succeeded.");
            fillTreeTableView(command.getSeries());
            updateTivoDetails(tivo);
        });
        task.setOnFailed(event -> {
            Throwable e = event.getSource().getException();
            logger.error("Error fetching recordings from {}: ", tivo.getName(), e);
            mainApp.clearStatusText();
            mainApp.showErrorMessage("Problem fetching list of recordings",
                    String.format("Unfortunately we encountered a problem while fetching the list of available " +
                            "recordings from %s. This usually means that either your computer or your TiVo has lost " +
                            "its network connection.%n%nError message: %s", tivo.getName(), e.getLocalizedMessage())
            );
            tivoIsBusy.setValue(false);
            enableUI();
        });
        mainApp.getRpcExecutor().submit(task);
    }

    private void fillTreeTableView(List<Series> series) {
        List<TreeTableColumn<Recording, ?>> oldSortOrder = recordingTreeTable.getSortOrder().stream().collect(Collectors.toList());
        TreeItem<Recording> root = new TreeItem<>(new Recording.Builder().seriesTitle("root").build());
        suggestions = new TreeItem<>(new Recording.Builder().seriesTitle("TiVo Suggestions")
                .isSeriesHeading(true).build());
        suggestions.expandedProperty().addListener(new HeaderExpandedHandler(suggestions));

        for (Series s : series) {
            List<Recording> recordings = s.getEpisodes();
            markArchivedRecordings(recordings);
            markQueuedRecordings(recordings);
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

    /**
     * Check each recording to see if it's in our archive history; if so, mark it as previously
     * archived and update its file location.
     */
    private void markArchivedRecordings(List<Recording> recordings) {
        ArchiveHistory archiveHistory = mainApp.getArchiveHistory();
        recordings.stream().filter(archiveHistory::contains).forEach(recording -> {
            ArchiveHistory.ArchiveHistoryItem historyItem = archiveHistory.get(recording);
            recording.setStatus(ArchiveStatus.FINISHED);
            recording.setDestination(historyItem.getLocation());
        });
    }

    /**
     * Check each recording to see if it's in our queue of archive tasks; if so, replace the recording
     * with a reference to our previously queued task.
     */
    private void markQueuedRecordings(List<Recording> recordings) {
        ArchiveQueueManager queueManager = mainApp.getArchiveQueueManager();
        for (int i = 0; i < recordings.size(); i++) {
            Recording recording = recordings.get(i);
            if (!recording.isSeriesHeading() && queueManager.containsRecording(recording)) {
                Recording queuedRecording = queueManager.getQueuedRecording(recording);
                recordings.set(i, queuedRecording);
            }
        }
    }

    public void updateTivoDetails(Tivo tivo) {
        MindCommandBodyConfigSearch bodyConfigSearch = new MindCommandBodyConfigSearch(tivo);
        MindTask bodyConfigTask = new MindTask(tivo.getClient(), bodyConfigSearch);
        bodyConfigTask.setOnSucceeded(event -> {
            updateStorageControls(tivo);
            mainApp.clearStatusText();
            tivoIsBusy.setValue(false);
            enableUI();
        });
        bodyConfigTask.setOnFailed(event -> {
            Throwable e = event.getSource().getException();
            logger.error("Error fetching details of {}: ", tivo.getName(), e);
            mainApp.clearStatusText();
            tivoIsBusy.setValue(false);
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
        startTivoSearchWithTimeout(TivoSearchTask.SEARCH_TIMEOUT_SHORT, TivoSearchTask.TIMEOUTS_BEFORE_PROMPT);
    }

    private void startTivoSearchWithTimeout(int timeout, int retries_before_prompt) {
        logger.debug("startTivoSearch()");
        assert (mainApp != null);

        removeTivoSelectedListener();
        trySearchAgain = false;
        if (tivoSearchTask == null) {
            tivoSearchTask = new TivoSearchTask(tivos, mainApp.getMak(), timeout);
            tivoSearchTask.setOnSucceeded(e -> {
                logger.debug("Tivo search task succeeded");
                if (tivoSearchTask.searchFailed()) {
                    logger.debug("Search task failed because of a network error");
                    mainApp.clearStatusText();
                    enableUI();
                    trySearchAgain = mainApp.showErrorMessageWithAction("We can't seem to access your network",
                            "Archivo encountered a problem when it tried to search for TiVos on your network.\n\n" +
                                    "This is usually caused by another program on your computer that is blocking " +
                                    "the network port Archivo needs to use.",
                            "Try Again");
                } else if (tivos.size() < 1) {
                    if (retries_before_prompt > 0) {
                        trySearchAgain = true;
                    } else {
                        logger.debug("Could not find any TiVos");
                        mainApp.clearStatusText();
                        enableUI();
                        trySearchAgain = mainApp.showErrorMessageWithAction("We didn't find any TiVos",
                                "Archivo couldn't find any TiVos on your network.\n\n" +
                                        "This may mean that your TiVo is too busy to respond, or that there's a problem with your network.",
                                "Try Again");
                    }
                } else {
                    Tivo lastDevice = mainApp.getLastDevice();
                    tivoList.getSelectionModel().clearSelection();
                    addTivoSelectedListener();
                    if (lastDevice != null && tivos.contains(lastDevice)) {
                        logger.info("Restoring previously used tivo: {}", lastDevice);
                        tivoList.getSelectionModel().select(lastDevice);
                    } else {
                        tivoList.getSelectionModel().selectFirst();
                    }
                }
                tivoSearchTask = null;
                if (trySearchAgain) {
                    startTivoSearchWithTimeout(TivoSearchTask.SEARCH_TIMEOUT_LONG, retries_before_prompt - 1);
                }
            });
            tivoSearchTask.setOnFailed(e -> {
                logger.error("Tivo search task failed: ", e.getSource().getException());
                tivoSearchTask = null;
            });
            tivoSearchTask.setOnCancelled(e -> {
                logger.info("Tivo search task canceled");
                tivoSearchTask = null;
            });
        }

        mainApp.setStatusText("Looking for TiVos...");
        disableUI();
        tivos.clear();

        mainApp.getRpcExecutor().submit(tivoSearchTask);
    }

    private void addTivoSelectedListener() {
        tivoList.getSelectionModel().selectedItemProperty().addListener(tivoSelectedListener);
    }

    private void removeTivoSelectedListener() {
        tivoList.getSelectionModel().selectedItemProperty().removeListener(tivoSelectedListener);
    }

    public void updateMak(String newMak) {
        restartTivoSearch();
        tivos.stream().forEach(tivo -> tivo.updateMak(newMak));
    }

    private void restartTivoSearch() {
        if (tivoSearchTask != null) {
            tivoSearchTask.cancel();
        }
        startTivoSearch();
    }

    /**
     * Disable the TiVo controls and the recording list
     */
    private void disableUI() {
        if (!uiDisabled) {
            logger.debug("Disabling UI");
            mainApp.getPrimaryStage().getScene().setCursor(Cursor.WAIT);
            uiDisabled = true;
        }
    }

    /**
     * Enable the TiVo controls and the recording list
     */
    private void enableUI() {
        if (uiDisabled) {
            logger.debug("Enabling UI");
            mainApp.getPrimaryStage().getScene().setCursor(Cursor.DEFAULT);
            uiDisabled = false;
        }
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
    private boolean removeRecordingFromList(Recording recording, ObservableList<TreeItem<Recording>> list) {
        Iterator<TreeItem<Recording>> i = list.iterator();

        while (i.hasNext()) {
            TreeItem<Recording> item = i.next();
            if (!item.isLeaf()) {
                if (removeRecordingFromList(recording, item.getChildren())) {
                    return true;
                }
            } else {
                Recording other = item.getValue();
                if (other.equals(recording)) {
                    promoteSingleElementGroup(item);
                    i.remove();
                    return true;
                }
            }
        }

        return false;
    }

    private void promoteSingleElementGroup(TreeItem<Recording> itemToRemove) {
        TreeItem<Recording> parent = itemToRemove.getParent();
        int siblings = parent.getChildren().size() - 1;
        logger.debug("Removed a recording, leaving {} sibling(s)", siblings);
        if (siblings == 1) {
            // Find the remaining sibling
            TreeItem<Recording> sibling = itemToRemove.nextSibling();
            if (sibling == null) {
                sibling = itemToRemove.previousSibling();
            }

            // Update the title to include series and episode information
            sibling.getValue().isChildRecording(false);

            // Replace the series group with the one remaining episode
            int parentIndex = findItemIndex(parent);
            TreeItem<Recording> grandParent = parent.getParent();
            grandParent.getChildren().remove(parent);
            grandParent.getChildren().add(parentIndex, sibling);
            recordingTreeTable.getSelectionModel().select(sibling);
            Platform.runLater(() -> {
                TreeItem<Recording> selectedItem = recordingTreeTable.getSelectionModel().getSelectedItem();
                int selectedIndex = recordingTreeTable.getRow(selectedItem);
                recordingTreeTable.getFocusModel().focus(selectedIndex);
            });
        }
    }

    private int findItemIndex(TreeItem<Recording> item) {
        ObservableList<TreeItem<Recording>> siblings = item.getParent().getChildren();
        return siblings.indexOf(item);
    }

    private void updateGroupStatus(TreeItem<Recording> group, ObservableList<TreeItem<Recording>> list) {
        if (!group.isLeaf()) {
            Iterator<TreeItem<Recording>> i = list.iterator();
            ArchiveStatus groupStatus = ArchiveStatus.EMPTY;
            while (i.hasNext()) {
                TreeItem<Recording> item = i.next();
                if (!item.isLeaf()) {
                    updateGroupStatus(item, item.getChildren());
                } else {
                    Recording recording = item.getValue();
                    if (groupStatus.compareTo(recording.getStatus()) > 0) {
                        groupStatus = recording.getStatus();
                    }
                }
            }
            group.getValue().setStatus(groupStatus);
        }
    }

    public void expandShows() {
        TreeItem<Recording> selectedItem = recordingTreeTable.getSelectionModel().getSelectedItem();
        expandTreeItemAndChildren(recordingTreeTable.getRoot());
        Platform.runLater(() -> {
            int selectedIndex = recordingTreeTable.getRow(selectedItem);
            recordingTreeTable.scrollTo(selectedIndex);
            recordingTreeTable.getSelectionModel().select(selectedIndex);
            recordingTreeTable.getFocusModel().focus(selectedIndex);
        });
    }

    public void collapseShows() {
        collapseTreeItemAndChildren(recordingTreeTable.getRoot());
        Platform.runLater(() -> {
            TreeItem<Recording> selectedItem = recordingTreeTable.getSelectionModel().getSelectedItem();
            int selectedIndex = recordingTreeTable.getRow(selectedItem);
            recordingTreeTable.scrollTo(selectedIndex);
            recordingTreeTable.getSelectionModel().select(selectedIndex);
            recordingTreeTable.getFocusModel().focus(selectedIndex);
        });
    }

    private void expandTreeItemAndChildren(TreeItem<Recording> item) {
        item.getChildren().stream().filter(child -> !child.isLeaf()).forEach(child -> {
            if (child != suggestions) {
                child.setExpanded(true);
            }
            expandTreeItemAndChildren(child);
        });
    }

    private void collapseTreeItemAndChildren(TreeItem<Recording> item) {
        item.getChildren().stream().filter(child -> !child.isLeaf()).forEach(child -> {
            if (child != suggestions) {
                child.setExpanded(false);
            }
            collapseTreeItemAndChildren(child);
        });
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
