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
import net.dropline.archivo.model.Recording;
import net.dropline.archivo.model.Tivo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

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
        tivos = FXCollections.observableArrayList();
        recordings = FXCollections.observableArrayList();

        // FIXME just for testing
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("adding first tivo");
                    // Add a Tivo
                    try {
                        InetAddress address = InetAddress.getByAddress(MainApp.testDeviceAddress);
                        Tivo tivo = new Tivo("TiVo", address, MainApp.testDeviceMAK);
                        tivos.add(tivo);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    Thread.sleep(10000);
                    System.out.println("adding second tivo");
                    InetAddress address = InetAddress.getByAddress(MainApp.testDeviceAddress);
                    Tivo tivo = new Tivo("TiVo 2", address, MainApp.testDeviceMAK);
                    tivos.add(tivo);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Add some test recordings
        recordings.add(new Recording.Builder().seriesTitle("NOVA").seriesNumber(20).channel("OPB", 710).recordedOn(LocalDateTime.of(2015, 7, 14, 21, 0)).
                episodeTitle("Chasing Pluto").episodeNumber(14).minutesLong(59).build());
        recordings.add(new Recording.Builder().seriesTitle("NOVA").seriesNumber(20).channel("OPB", 710).recordedOn(LocalDateTime.of(2015, 3, 5, 19, 30)).
                episodeTitle("The Great Math Mystery").episodeNumber(12).minutesLong(59).build());
        recordings.add(new Recording.Builder().seriesTitle("Doctor Who").seriesNumber(4).channel("BBC America", 790).
                episodeTitle("Silence in the Library").episodeNumber(8).minutesLong(60).recordedOn(LocalDateTime.of(2010, 9, 20, 20, 0)).build());
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

        tivos.addListener(new TivoListChangeListener());
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
