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

package net.straylightlabs.archivo.controller;

import javafx.application.Platform;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegOutputReader extends ProcessOutputReader {
    private int duration;
    private final ArchiveStatus.TaskStatus task;

    private static final Pattern DURATION = Pattern.compile("Duration: ([\\d]+):([\\d]+):([\\d+])");
    private static final Pattern CURRENT_TIME = Pattern.compile("time=([\\d]+):([\\d]+):([\\d+])");

    public FFmpegOutputReader(Recording recording, ArchiveStatus.TaskStatus task) {
        super(recording);
        this.task = task;
    }

    @Override
    public void processLine(String line) {
        if (task == ArchiveStatus.TaskStatus.REMUXING) {
            if (duration == 0) {
                Matcher matcher = DURATION.matcher(line);
                if (matcher.find()) {
                    int hours = Integer.parseInt(matcher.group(1)) * 60 * 60;
                    int minutes = Integer.parseInt(matcher.group(2)) * 60;
                    duration = Integer.parseInt(matcher.group(3)) + hours + minutes;
                }
            } else {
                Matcher matcher = CURRENT_TIME.matcher(line);
                if (matcher.find()) {
                    int hours = Integer.parseInt(matcher.group(1)) * 60 * 60;
                    int minutes = Integer.parseInt(matcher.group(2)) * 60;
                    int currentSeconds = Integer.parseInt(matcher.group(3)) + hours + minutes;
                    double percentComplete = currentSeconds / (double) duration;

//                    switch (task) {
//                        case REMUXING:
                    Platform.runLater(() -> recording.setStatus(
                                    ArchiveStatus.createRemuxingStatus(percentComplete, getSecondsRemaining(percentComplete)))
                    );
//                            break;
//                    case REMOVING_COMMERCIALS:
//                        Archivo.logger.info("Removing commercials progress: {} time remaining: {}", percentComplete, getSecondsRemaining(percentComplete));
//                        Platform.runLater(() -> recording.statusProperty().setValue(
//                                        ArchiveStatus.createRemovingCommercialsStatus(percentComplete, getSecondsRemaining(percentComplete)))
//                        );
//                        break;
                }
            }
        }
    }
}
