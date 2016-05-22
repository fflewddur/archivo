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

import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveHistory;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.Tivo;

import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Enqueue archive requests for processing via a background thread, and allow archive tasks to be canceled.
 * Alerts its observes when the queue size changes between empty and not-empty.
 */
public class ArchiveQueueManager extends Observable {
    private final Archivo mainApp;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<Recording, ArchiveTask> queuedTasks;
    private final Lock downloadLock;
    private final Lock processingLock;

    private final int POOL_SIZE = 2;

    public ArchiveQueueManager(Archivo mainApp) {
        this.mainApp = mainApp;
        executorService = Executors.newFixedThreadPool(POOL_SIZE);
        downloadLock = new ReentrantLock();
        processingLock = new ReentrantLock();
        queuedTasks = new ConcurrentHashMap<>();
    }

    public boolean enqueueArchiveTask(Recording recording, Tivo tivo, String mak) {
        try {
            ArchiveTask task = new ArchiveTask(recording, tivo, mak, mainApp.getUserPrefs(), downloadLock, processingLock);
            task.setOnRunning(event -> mainApp.setStatusText(String.format("Archiving %s...", recording.getFullTitle())));
            task.setOnSucceeded(event -> {
                Archivo.logger.info("ArchiveTask succeeded for {}", recording.getFullTitle());
                updateArchiveHistory(recording);
                removeTask(recording);
                recording.setStatus(ArchiveStatus.FINISHED);
            });
            task.setOnFailed(event -> {
                Throwable e = event.getSource().getException();
                Archivo.logger.error("ArchiveTask failed for {}: ", recording.getFullTitle(), e);
                e.printStackTrace();
                removeTask(recording);
                recording.setStatus(ArchiveStatus.createErrorStatus(e));
                Archivo.telemetryController.sendArchiveFailedEvent(e);
            });
            task.setOnCancelled(event -> {
                Archivo.logger.info("ArchiveTask canceled for {}", recording.getFullTitle());
                removeTask(recording);
                recording.setStatus(ArchiveStatus.EMPTY);
            });
            Archivo.logger.info("Submitting task to executor service: {}", executorService);
            if (!hasTasks()) {
                setChanged();
                notifyObservers(true);
            }
            queuedTasks.put(recording, task);
            executorService.submit(task);
        } catch (RejectedExecutionException e) {
            Archivo.logger.error("Could not schedule archive task: ", e);
            return false;
        }
        return true;
    }

    private void removeTask(Recording recording) {
        queuedTasks.remove(recording);
        if (!hasTasks()) {
            setChanged();
            notifyObservers(false);
        }
        mainApp.clearStatusText();
    }

    private void updateArchiveHistory(Recording recording) {
        ArchiveHistory archiveHistory = mainApp.getArchiveHistory();
        archiveHistory.add(recording);
        archiveHistory.save();
    }

    public void cancelArchiveTask(Recording recording) {
        ArchiveTask task = queuedTasks.get(recording);
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

    public boolean containsRecording(Recording recording) {
        return queuedTasks.containsKey(recording);
    }

    public Recording getQueuedRecording(Recording recording) {
        ArchiveTask task = queuedTasks.get(recording);
        if (task == null) {
            throw new IllegalArgumentException("recording is not in queuedTasks");
        }
        return task.getRecording();
    }
}
