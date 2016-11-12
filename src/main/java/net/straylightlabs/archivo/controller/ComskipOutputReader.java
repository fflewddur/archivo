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

package net.straylightlabs.archivo.controller;

import javafx.application.Platform;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComskipOutputReader extends ProcessOutputReader {
    private static final Pattern PERCENT = Pattern.compile("([\\d\\.]+)%");

    public ComskipOutputReader(Recording recording) {
        super(recording);
    }

    @Override
    public void processLine(String line) {
        Matcher matcher = PERCENT.matcher(line);
        if (matcher.find()) {
            double percentComplete = Double.parseDouble(matcher.group(1)) * .01;
            Platform.runLater(() -> recording.setStatus(
                    ArchiveStatus.createFindingCommercialsStatus(percentComplete, getSecondsRemaining(percentComplete)))
            );
        }
    }
}
