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

import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;

public abstract class ProcessOutputReader implements Runnable {
    protected final Recording recording;
    private final LocalDateTime startTime;
    private InputStream inputStream;

    private final static double MIN_PROGRESS_FOR_ETA = 0.03;

    public ProcessOutputReader(Recording recording) {
        this.recording = recording;
        startTime = LocalDateTime.now();
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (Thread.interrupted()) {
                    Archivo.logger.info("ProcessOutputReader interrupted");
                    return;
                }
                processLine(line);
            }
        } catch (IOException e) {
            Archivo.logger.error("IOException in ProcessOutputReader: ", e);
        }
    }

    public abstract void processLine(String line);

    // TODO add a queue of the N most recent ETAs; result of this method should be average of those
    protected int getSecondsRemaining(double progress) {
        if (progress > MIN_PROGRESS_FOR_ETA) {
            if (progress > 1) {
                progress = .99;
            }
            long elapsedSeconds = Duration.between(startTime, LocalDateTime.now()).getSeconds();
            double progressPerSecond = progress / elapsedSeconds;
//            Archivo.logger.info("Elapsed seconds = {}, progressPerSecond = {}, secondsRemaining", elapsedSeconds, progressPerSecond);
            return (int) ((1.0 - progress) / progressPerSecond);
        } else {
            return ArchiveStatus.TIME_UNKNOWN;
        }
    }
}
