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

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import net.dropline.archivo.MainApp;
import net.dropline.archivo.model.Recording;

public class RecordingListController {
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

    @FXML
    private void initialize() {
        showColumn.setCellValueFactory(cellData -> cellData.getValue().seriesTitleProperty());
        episodeColumn.setCellValueFactory(cellData -> cellData.getValue().episodeTitleProperty());
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateRecordedProperty().asString());
        durationColumn.setCellValueFactory(cellData -> cellData.getValue().durationProperty().asString());
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;

        recordingTable.setItems(this.mainApp.getRecordings());
    }
}
