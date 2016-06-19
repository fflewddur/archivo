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

import javafx.beans.property.ObjectProperty;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import net.straylightlabs.archivo.model.FileType;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.UserPrefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Prompt the user for the archive destination path.
 */
public class SaveFileDialog {
    private final FileChooser fileChooser;
    private final Window parent;
    private final Recording recording;
    private final UserPrefs userPrefs;
    private boolean rememberDestinationFolder;

    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(SaveFileDialog.class);

    public SaveFileDialog(Window parent, Recording recording, UserPrefs userPrefs) {
        fileChooser = new FileChooser();
        this.parent = parent;
        this.recording = recording;
        this.userPrefs = userPrefs;
        initDialog();
    }

    private void initDialog() {
        setupFileTypes(fileChooser);
        Path destination = recording.getDestination();
        if (destination != null) {
            // If we already have a destination, it means this dialog is for renaming conflicting files;
            // don't save the folder for these cases
            rememberDestinationFolder = false;
            fileChooser.setInitialFileName(destination.getFileName().toString());
            fileChooser.setInitialDirectory(destination.getParent().toFile());
        } else {
            rememberDestinationFolder = true;
            String defaultFilename = recording.getDefaultFlatFilename();
            fileChooser.setInitialFileName(defaultFilename);
            fileChooser.setInitialDirectory(userPrefs.getLastFolder().toFile());
        }
    }

    public boolean showAndWait() {
        ObjectProperty<FileChooser.ExtensionFilter> selectedExtensionFilterProperty =
                fileChooser.selectedExtensionFilterProperty();
        File destination = fileChooser.showSaveDialog(parent);
        FileType type = saveFileType(selectedExtensionFilterProperty);
        if (destination != null) {
            Path lastFolder = destination.toPath();
            recording.setDestination(destination.toPath());
            recording.setDestinationType(type);
            if (rememberDestinationFolder) {
                userPrefs.setLastFolder(lastFolder.getParent());
            }
            return true;
        }
        return false;
    }

    private void setupFileTypes(FileChooser chooser) {
        List<FileChooser.ExtensionFilter> fileTypes = new ArrayList<>();
        FileChooser.ExtensionFilter selected = null;
        String previousFileType = userPrefs.getMostRecentFileType();
        for (FileType type : FileType.values()) {
            FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(type.getDescription(), type.getExtension());
            fileTypes.add(filter);
            if (type.getDescription().equalsIgnoreCase(previousFileType)) {
                logger.info("Setting extension filter: {}", previousFileType);
                selected = filter;
            }
        }
        chooser.getExtensionFilters().addAll(fileTypes);
        if (selected != null) {
            chooser.setSelectedExtensionFilter(selected);
        }
    }

    private FileType saveFileType(ObjectProperty<FileChooser.ExtensionFilter> selectedExtensionFilterProperty) {
        FileChooser.ExtensionFilter filter = selectedExtensionFilterProperty.get();
        FileType fileType = null;
        if (filter != null) {
            String description = filter.getDescription();
            logger.info("Selected extension filter: {}", description);
            fileType = FileType.fromDescription(description);
            userPrefs.setMostRecentType(fileType);
        }
        return fileType;
    }
}
