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
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.Tivo;
import net.straylightlabs.archivo.net.MindCommandIdSearch;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

/**
 * Created by todd on 8/24/15.
 */
public class ArchiveQueueManager implements Runnable {
    private final BlockingQueue<Recording> archiveQueue;
    private final Archivo mainApp;

    public ArchiveQueueManager(Archivo mainApp, BlockingQueue<Recording> queue) {
        this.mainApp = mainApp;
        archiveQueue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                archive(archiveQueue.take());
            }
        } catch (InterruptedException e) {
            Archivo.logger.severe("Interrupted while retrieving next archive task: " + e.getLocalizedMessage());
        }
    }

    private void archive(Recording recording) {
        Archivo.logger.info("Starting archive task for " + recording.getTitle());
        try {
            Tivo tivo = mainApp.getActiveTivo();
            MindCommandIdSearch command = new MindCommandIdSearch(recording, tivo);
            command.executeOn(tivo.getClient());
            URL url = command.getDownloadUrl();
            Archivo.logger.info("URL: " + url);
        } catch (IOException e) {
            Archivo.logger.severe("Error fetching recording information: " + e.getLocalizedMessage());
        }
    }
}
