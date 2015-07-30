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

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.Series;
import net.straylightlabs.archivo.model.Tivo;
import net.straylightlabs.archivo.net.MindCommandRecordingFolderItemSearch;
import net.straylightlabs.archivo.net.MindTask;
import net.straylightlabs.archivo.net.TivoSearchTask;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class RecordingListController implements Initializable {
    private final ObservableList<Tivo> tivos;
    private TivoSearchTask tivoSearchTask;

    @FXML
    private HBox toolbar;
    @FXML
    private ComboBox<Tivo> tivoList;
    @FXML
    private TreeTableView<Recording> recordingTreeTable;
    @FXML
    private TreeTableColumn<Recording, String> showColumn;
    @FXML
    private TreeTableColumn<Recording, String> episodeColumn;
    @FXML
    private TreeTableColumn<Recording, LocalDateTime> dateColumn;

    private Archivo mainApp;

    public RecordingListController(Archivo mainApp, List<Tivo> initialTivos) {
        this.mainApp = mainApp;
        tivos = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(initialTivos));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tivoList.setConverter(new Tivo.StringConverter());
        tivoList.getSelectionModel().selectedItemProperty().addListener(
                (tivoList, oldTivo, curTivo) -> {
                    mainApp.setLastDevice(curTivo);
                    fetchRecordingsFrom(curTivo);
                }
        );

        showColumn.setCellValueFactory(data -> data.getValue().getValue().seriesTitleProperty());
        episodeColumn.setCellValueFactory(data -> data.getValue().getValue().episodeTitleProperty());
        dateColumn.setCellValueFactory(data -> data.getValue().getValue().dateRecordedProperty());
        dateColumn.setCellFactory(col -> new RecordedOnCellFactory());

        tivoList.setItems(tivos);

        // When the list of TiVos is first populated, automatically select one
        tivos.addListener(new TivoListChangeListener());

        Tivo lastDevice = mainApp.getLastDevice();
        if (lastDevice != null && tivos.contains(lastDevice)) {
            tivoList.setValue(lastDevice);
        }
    }

    @SuppressWarnings("unused")
    public void fetchRecordingsFromSelectedTivo() {
        fetchRecordingsFrom(tivoList.getValue());
    }

    private void fetchRecordingsFrom(Tivo tivo) {
        mainApp.setStatusText("Fetching recordings...");
        recordingTreeTable.setDisable(true);

        MindCommandRecordingFolderItemSearch command = new MindCommandRecordingFolderItemSearch();
        MindTask task = new MindTask(tivo.getClient(), command);
        task.setOnSucceeded(event -> {
            fillTreeTableView(command.getSeries());
            mainApp.clearStatusText();
            recordingTreeTable.setDisable(false);
        });
        task.setOnFailed(event -> System.err.format("Error fetching recordings from %s: %s%n", tivo.getName(),
                event.getSource().getException().getLocalizedMessage()));
        mainApp.getExecutor().submit(task);
    }

    private void fillTreeTableView(List<Series> series) {
        TreeItem<Recording> root = new TreeItem<>(new Recording.Builder().seriesTitle("root").build());
        for (Series s : series) {
            List<Recording> recordings = s.getEpisodes();
            TreeItem<Recording> item;
            if (recordings.size() > 1) {
                // Create a new tree node with children
                // TODO Sort the recordings by date so we ensure recordedOn is set with the most-recent date
                item = new TreeItem<>(new Recording.Builder().seriesTitle(s.getTitle())
                        .recordedOn(recordings.get(0).getDateRecorded()).isSeriesHeading(true).build());
                for (Recording recording : s.getEpisodes()) {
                    item.getChildren().add(new TreeItem<>(recording));
                }
                item.setExpanded(true);
            } else {
                // Don't create children for this node
                item = new TreeItem<>(recordings.get(0));
            }

            root.getChildren().add(item);
        }
        recordingTreeTable.setRoot(root);
        recordingTreeTable.setShowRoot(false);
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

        mainApp.getExecutor().submit(tivoSearchTask);
    }

    private void disableUI() {
        setUIDisabled(true);
    }

    private void enableUI() {
        setUIDisabled(false);
    }

    private void setUIDisabled(boolean disabled) {
        toolbar.setDisable(disabled);
        recordingTreeTable.setDisable(disabled);
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
}
