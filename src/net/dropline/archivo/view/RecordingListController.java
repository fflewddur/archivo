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

package net.dropline.archivo.view;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import net.dropline.archivo.Archivo;
import net.dropline.archivo.model.Recording;
import net.dropline.archivo.model.Tivo;
import net.dropline.archivo.net.MindCommandRecordingFolderItemSearch;
import net.dropline.archivo.net.MindTask;
import net.dropline.archivo.net.TivoSearchTask;

import java.net.URL;
import java.util.ResourceBundle;

public class RecordingListController implements Initializable {
    private final ObservableList<Tivo> tivos;
    private final ObservableList<Recording> recordings;
    private TivoSearchTask tivoSearchTask;

    @FXML
    private HBox toolbar;
    @FXML
    private ComboBox<Tivo> tivoList;
    @FXML
    private TableView<Recording> recordingTable;
    @FXML
    private TableColumn<Recording, String> showColumn;
    @FXML
    private TableColumn<Recording, String> episodeColumn;
    @FXML
    private TableColumn<Recording, String> dateColumn;
    @FXML
    private TableColumn<Recording, String> durationColumn;

    private Archivo mainApp;

    public RecordingListController() {
        tivos = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        recordings = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        disableUI();

        tivoList.setConverter(new Tivo.StringConverter());
        tivoList.getSelectionModel().selectedItemProperty().addListener(
                (tivoList, oldTivo, curTivo) -> {
                    fetchRecordingsFrom(curTivo);
                }
        );

        showColumn.setCellValueFactory(cellData -> cellData.getValue().seriesTitleProperty());
        episodeColumn.setCellValueFactory(cellData -> cellData.getValue().episodeTitleProperty());
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateRecordedProperty().asString());
        durationColumn.setCellValueFactory(cellData -> cellData.getValue().durationProperty().asString());

        recordingTable.setItems(recordings);
        tivoList.setItems(tivos);

        // When the list of TiVos is first populated, automatically select one
        tivos.addListener(new TivoListChangeListener());
    }

    public void fetchRecordingsFromSelectedTivo() {
        fetchRecordingsFrom(tivoList.getValue());
    }

    private void fetchRecordingsFrom(Tivo tivo) {
        mainApp.setStatusText("Fetching recordings...");
        recordings.clear();
        recordingTable.setDisable(true);

        MindCommandRecordingFolderItemSearch command = new MindCommandRecordingFolderItemSearch();
        MindTask task = new MindTask(tivo.getClient(), command);
        task.setOnSucceeded(event -> {
            recordings.addAll(command.getRecordings());
            mainApp.clearStatusText();
            recordingTable.setDisable(false);
        });
        task.setOnFailed(event -> {
            System.err.format("Error fetching recordings from %s: %s%n", tivo.getName(),
                    event.getSource().getException().getLocalizedMessage());
        });
        mainApp.getExecutor().submit(task);
    }

    public void startTivoSearch() {
        assert (mainApp != null);

        if (tivoSearchTask == null) {
            tivoSearchTask = new TivoSearchTask(tivos, mainApp.getMak());
        }

        mainApp.setStatusText("Looking for TiVos...");
//        try {
//            tivoSearchTask.startSearch();
//        } catch (IOException e) {
//            System.err.println(e.getLocalizedMessage());
//            e.printStackTrace();
//        }
//        tivoSearchTask.setOnSucceeded(event -> {
            // Add any new TiVos to our list
//            @SuppressWarnings("unchecked") Set<Tivo> found = (Set<Tivo>) event.getSource().getValue();
//            List<Tivo> toAdd = found.stream().filter(t -> !tivos.contains(t)).collect(Collectors.toList());
//            tivos.addAll(toAdd);
//            mainApp.clearStatusText();
//        });
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
        recordingTable.setDisable(disabled);
    }

    public void setMainApp(Archivo mainApp) {
        this.mainApp = mainApp;
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
        }
    }
}
