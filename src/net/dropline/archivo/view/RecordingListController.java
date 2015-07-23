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
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import net.dropline.archivo.MainApp;
import net.dropline.archivo.controller.TivoSearchService;
import net.dropline.archivo.model.Recording;
import net.dropline.archivo.model.Tivo;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordingListController {
    private final ObservableList<Tivo> tivos;
    private final ObservableList<Recording> recordings;

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

    private MainApp mainApp;

    public RecordingListController() {
        tivos = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        recordings = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    }

    @FXML
    private void initialize() {
        tivoList.setConverter(new Tivo.StringConverter());
        tivoList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    recordings.clear();
                    recordings.addAll(newValue.getRecordings());
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

        // Start looking for TiVo devices
        startTivoSearch();
    }

    private void startTivoSearch() {
        try {
            System.out.println("Start tivo search...");
            TivoSearchService searchService = new TivoSearchService();
            searchService.setOnSucceeded(event -> {
                Set<Tivo> found = (Set<Tivo>) event.getSource().getValue();
                List<Tivo> toAdd = found.stream().filter(t -> !tivos.contains(t)).collect(Collectors.toList());
                tivos.addAll(toAdd);
            });
            searchService.start();
        } catch (IOException e) {
            System.err.println("Error searching for TiVo devices: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void setMainApp(MainApp mainApp) {
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
                    }
                }
            }
        }
    }
}
