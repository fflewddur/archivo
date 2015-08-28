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
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.Tivo;
import net.straylightlabs.archivo.net.MindCommandIdSearch;
import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;

/**
 * Handle each archive request.
 */
public class ArchiveQueueManager implements Runnable {
    private final BlockingQueue<Recording> archiveQueue;
    private final Archivo mainApp;

    private static final int BUFFER_SIZE = 8192;
    private static final double MIN_PROGRESS_INCREMENT = 0.01;

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
        Platform.runLater(() -> {
            mainApp.setStatusText(String.format("Archiving %s...", recording.getTitle()));
            recording.statusProperty().setValue(ArchiveStatus.createDownloadingStatus(0, ArchiveStatus.TIME_UNKNOWN));
        });
        try {
            Tivo tivo = mainApp.getActiveTivo();
            MindCommandIdSearch command = new MindCommandIdSearch(recording, tivo);
            command.executeOn(tivo.getClient());
            URL url = command.getDownloadUrl();
            Archivo.logger.info("URL: " + url);

            // FIXME Make this user-configurable
            Path destination = Paths.get(System.getProperty("user.home"), "download.tivo");
            Archivo.logger.info("Saving file to " + destination);
            getRecording(recording, url, destination);
        } catch (IOException e) {
            Archivo.logger.severe("Error fetching recording information: " + e.getLocalizedMessage());
        }
        Platform.runLater(() -> {
            mainApp.clearStatusText();
            recording.statusProperty().setValue(ArchiveStatus.FINISHED);
        });
    }

    private void getRecording(Recording recording, URL url, Path destination) {
        try (CloseableHttpClient client = buildHttpClient()) {
            HttpGet get = new HttpGet(url.toString());
            // Initial request to set the session cookie
            try (CloseableHttpResponse response = client.execute(get)) {
                response.close(); // Not needed, but clears up a warning
            }
            // Now fetch the file
            try (CloseableHttpResponse response = client.execute(get)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    Archivo.logger.severe("Error downloading recording: " + response.getStatusLine());
                }

                Archivo.logger.info("Status line: " + response.getStatusLine());
                handleResponse(response, destination, recording);
            }
        } catch (IOException e) {
            Archivo.logger.severe("Error downloading recording: " + e.getLocalizedMessage());
        }
    }

    private CloseableHttpClient buildHttpClient() {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials("tivo", mainApp.getMak()));
        CookieStore cookieStore = new BasicCookieStore();
        ConnectionConfig connConfig = ConnectionConfig.custom().setBufferSize(BUFFER_SIZE).build();
        return HttpClients.custom()
                .useSystemProperties()
                .setDefaultConnectionConfig(connConfig)
                .setDefaultCredentialsProvider(credsProvider)
                .setDefaultCookieStore(cookieStore)
                .setUserAgent(Archivo.USER_AGENT)
                .build();
    }

    private void handleResponse(CloseableHttpResponse response, Path destination, Recording recording) {
        long estimatedLength = getEstimatedLengthFromHeaders(response);
        double priorPercent = 0;
        try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(destination))) {
            try (BufferedInputStream inputStream = new BufferedInputStream(response.getEntity().getContent(), BUFFER_SIZE)) {
                byte[] buffer = new byte[BUFFER_SIZE + 1];
                long totalBytesRead = 0;
                LocalDateTime startTime = LocalDateTime.now();
                for (int bytesRead = inputStream.read(buffer, 0, BUFFER_SIZE);
                     bytesRead >= 0;
                     bytesRead = inputStream.read(buffer, 0, BUFFER_SIZE)) {
                    totalBytesRead += bytesRead;
//                            Archivo.logger.info("Bytes read: " + bytesRead);
                    outputStream.write(buffer, 0, bytesRead);
                    double percent = totalBytesRead / (double) estimatedLength;
                    if (percent - priorPercent >= MIN_PROGRESS_INCREMENT) {
                        Duration elapsedTime = Duration.between(startTime, LocalDateTime.now());
                        double kbs = (totalBytesRead / 1024) / elapsedTime.getSeconds();
                        long kbRemaining = (estimatedLength - totalBytesRead) / 1024;
                        int secondsRemaining = (int) (kbRemaining / kbs);
                        Archivo.logger.info(String.format("Read %d bytes of %d expected bytes (%d%%) in %s (%.1f KB/s)",
                                totalBytesRead, estimatedLength, (int) (percent * 100), elapsedTime, kbs));
                        Platform.runLater(() -> recording.statusProperty().setValue(
                                ArchiveStatus.createDownloadingStatus(percent, secondsRemaining)
                        ));
                        priorPercent = percent;
                    }
                }
                Archivo.logger.info("Download complete.");
            } catch (IOException e) {
                Archivo.logger.severe("Error reading file from network: " + e.getLocalizedMessage());
            }
        } catch (IOException e) {
            Archivo.logger.severe("Error creating file: " + e.getLocalizedMessage());
        }
    }

    private long getEstimatedLengthFromHeaders(CloseableHttpResponse response) {
        long length = -1;
        for (Header header : response.getAllHeaders()) {
            if (header.getName().equalsIgnoreCase("TiVo-Estimated-Length")) {
                try {
                    length = Long.parseLong(header.getValue());
                } catch (NumberFormatException e) {
                    Archivo.logger.severe(String.format("Error parsing estimated length (%s): %s%n",
                                    header.getValue(), e.getLocalizedMessage())
                    );
                }
            }
        }
        return length;
    }
}
