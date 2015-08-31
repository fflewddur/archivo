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

import javafx.concurrent.Task;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.Tivo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Enqueue archive requests for processing via a background thread, and allow archive tasks to be canceled.
 */
public class ArchiveQueueManager {
    private final Archivo mainApp;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<Recording, Task<Recording>> queuedTasks;

    public ArchiveQueueManager(Archivo mainApp) {
        this.mainApp = mainApp;
        executorService = Executors.newSingleThreadExecutor();
        queuedTasks = new ConcurrentHashMap<>();
    }

    public boolean enqueueArchiveTask(Recording recording, Tivo tivo, String mak) {
        try {
            Path destination = getFileDestination();
            ArchiveTask task = new ArchiveTask(recording, tivo, mak, destination);
            task.setOnRunning(event -> {
                mainApp.setStatusText(String.format("Archiving %s...", recording.getTitle()));
                recording.statusProperty().setValue(ArchiveStatus.createDownloadingStatus(0, ArchiveStatus.TIME_UNKNOWN));
            });
            task.setOnSucceeded(event -> {
                queuedTasks.remove(recording);
                mainApp.clearStatusText();
                recording.statusProperty().setValue(ArchiveStatus.FINISHED);
            });
            task.setOnFailed(event -> {
                queuedTasks.remove(recording);
                mainApp.clearStatusText();
                recording.statusProperty().setValue(ArchiveStatus.ERROR);
            });
            task.setOnCancelled(event -> {
                queuedTasks.remove(recording);
                mainApp.clearStatusText();
                recording.statusProperty().setValue(ArchiveStatus.EMPTY);
            });
            Archivo.logger.info("Submitting task to executor service: " + executorService);
            executorService.submit(task);
            queuedTasks.put(recording, task);
        } catch (RejectedExecutionException e) {
            Archivo.logger.severe("Could not schedule archive task: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public void cancelArchiveTask(Recording recording) {
        Task<Recording> task = queuedTasks.get(recording);
        if (task != null) {
            task.cancel();
        }
    }

    public void cancelAllArchiveTasks() {
        queuedTasks.forEach(((recording, task) -> task.cancel()));
    }

    public boolean hasTasks() {
        return queuedTasks.size() > 0;
    }

    private Path getFileDestination() {
        // TODO Make this user-configurable
        Path destination = Paths.get(System.getProperty("user.home"), "download.TiVo");
        return destination;
    }
}
