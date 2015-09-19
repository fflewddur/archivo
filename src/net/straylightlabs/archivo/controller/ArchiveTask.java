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
import javafx.concurrent.Task;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.Tivo;
import net.straylightlabs.archivo.net.MindCommandIdSearch;
import net.straylightlabs.tivolibre.TivoDecoder;
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

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Handle the tasks of fetching the recording file from a TiVo, decrypting it, and transcoding it.
 */
public class ArchiveTask extends Task<Recording> {
    private final Recording recording;
    private final Tivo tivo;
    private final String mak;

    private static final int BUFFER_SIZE = 8192; // 8KB
    private static final int PIPE_BUFFER_SIZE = 1024 * 1024; // 1MB
    private static final int MIN_PROGRESS_INCREMENT = 10 * 1024 * 1024; // number of bytes that must transfer before we update our progress
    private static final int NUM_RETRIES = 5;
    private static final int RETRY_DELAY = 5000; // delay between retry attempts, in ms
    private static final double ESTIMATED_SIZE_THRESHOLD = 0.8; // Need to download this % of a file to consider it successful

    static {
        TivoDecoder.setLogger(Archivo.logger);
    }

    public ArchiveTask(Recording recording, Tivo tivo, String mak) {
        this.recording = recording;
        this.tivo = tivo;
        this.mak = mak;
    }

    @Override
    protected Recording call() throws ArchiveTaskException {
        archive();
        return recording;
    }

    private void archive() throws ArchiveTaskException {
        if (isCancelled()) {
            Archivo.logger.info("ArchiveTask canceled by user.");
            return;
        }

        Archivo.logger.info("Starting archive task for " + recording.getTitle());
        try {
            MindCommandIdSearch command = new MindCommandIdSearch(recording, tivo);
            command.executeOn(tivo.getClient());
            URL url = command.getDownloadUrl();
            Archivo.logger.info("URL: " + url);
            Archivo.logger.info("Saving file to " + recording.getDestination());
            getRecording(recording, url);
        } catch (IOException e) {
            Archivo.logger.severe("Error fetching recording information: " + e.getLocalizedMessage());
            throw new ArchiveTaskException("Problem fetching recording information");
        }
    }

    private void getRecording(Recording recording, URL url) throws ArchiveTaskException {
        if (isCancelled()) {
            Archivo.logger.info("ArchiveTask canceled by user.");
            return;
        }

        try (CloseableHttpClient client = buildHttpClient()) {
            HttpGet get = new HttpGet(url.toString());
            // Initial request to set the session cookie
            try (CloseableHttpResponse response = client.execute(get)) {
                response.close(); // Not needed, but clears up a warning
            }
            // Now fetch the file
            int retries = NUM_RETRIES;
            int retryDelay = RETRY_DELAY;
            boolean responseOk = false;
            while (!responseOk && retries > 0) {
                try (CloseableHttpResponse response = client.execute(get)) {
                    if (response.getStatusLine().getStatusCode() != 200) {
                        Archivo.logger.severe("Error downloading recording: " + response.getStatusLine());
                        Archivo.logger.info("Sleeping for " + retryDelay + " ms");
                        Thread.sleep(retryDelay);
                        retryDelay += RETRY_DELAY;
                        retries--;
                    } else {
                        Archivo.logger.info("Status line: " + response.getStatusLine());
                        responseOk = true;
                        handleResponse(response, recording);
                    }
                } catch (InterruptedException e) {
                    Archivo.logger.severe("Thread interrupted: " + e.getLocalizedMessage());
                }
            }
            if (!responseOk) {
                throw new ArchiveTaskException("Problem downloading recording");
            }
        } catch (IOException e) {
            Archivo.logger.severe("Error downloading recording: " + e.getLocalizedMessage());
            throw new ArchiveTaskException("Problem downloading recording");
        }
    }

    private CloseableHttpClient buildHttpClient() {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials("tivo", mak));
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

    private void handleResponse(CloseableHttpResponse response, Recording recording) throws ArchiveTaskException {
        long estimatedLength = getEstimatedLengthFromHeaders(response);
        boolean decrypt = shouldDecrypt(recording);
        try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(recording.getDestination()));
             BufferedInputStream inputStream = new BufferedInputStream(response.getEntity().getContent(), BUFFER_SIZE);
             PipedInputStream pipedInputStream = new PipedInputStream(PIPE_BUFFER_SIZE);
             PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream)
        ) {
            Archivo.logger.info("Starting download...");
            // Pipe the network stream to our TiVo decoder, then pipe the output of that to the output file stream
            Thread thread = null;
            if (decrypt) {
                thread = new Thread(() -> {
                    TivoDecoder decoder = new TivoDecoder(pipedInputStream, outputStream, mak);
                    if (!decoder.decode()) {
                        Archivo.logger.severe("Failed to decode file");
                        throw new ArchiveTaskException("Problem decoding recording");
                    }
                });
                thread.start();
            }

            byte[] buffer = new byte[BUFFER_SIZE + 1];
            long totalBytesRead = 0;
            long priorBytesRead = 0;
            LocalDateTime startTime = LocalDateTime.now();
            for (int bytesRead = inputStream.read(buffer, 0, BUFFER_SIZE);
                 bytesRead >= 0;
                 bytesRead = inputStream.read(buffer, 0, BUFFER_SIZE)) {
                if (decrypt && !thread.isAlive()) {
                    Archivo.logger.severe("Decoding thread died prematurely");
                    response.close();
                    throw new ArchiveTaskException("Problem decoding recording");
                }
                if (isCancelled()) {
                    Archivo.logger.info("ArchiveTask cancelled by user.");
                    response.close(); // Stop the network transaction
                    return;
                }
                totalBytesRead += bytesRead;
                Archivo.logger.fine("Bytes read: " + bytesRead);

                if (decrypt) {
                    pipedOutputStream.write(buffer, 0, bytesRead);
                } else {
                    outputStream.write(buffer, 0, bytesRead);
                }

                if (totalBytesRead > priorBytesRead + MIN_PROGRESS_INCREMENT) {
                    double percent = totalBytesRead / (double) estimatedLength;
                    updateProgress(recording, percent, startTime, totalBytesRead, estimatedLength);
                    priorBytesRead = totalBytesRead;
                    Archivo.logger.info("Total bytes read from network: " + totalBytesRead);
                }
            }
            Archivo.logger.info("Download finished.");

            if (decrypt) {
                // Close the pipe to ensure the decoding thread finishes
                pipedOutputStream.flush();
                pipedOutputStream.close();
                // Wait for the decoding thread to finish
                thread.join();
                Archivo.logger.info("Decoding finished.");
            }

            verifyDownloadSize(totalBytesRead, estimatedLength);
        } catch (IOException e) {
            Archivo.logger.severe("IOException: " + e.getLocalizedMessage());
            throw new ArchiveTaskException("Problem downloading recording");
        } catch (InterruptedException e) {
            Archivo.logger.severe("Decoding thread interrupted: " + e.getLocalizedMessage());
        }
    }

    /**
     * If the user chose to save the file as a .TiVo, don't decrypt it.
     */
    private boolean shouldDecrypt(Recording recording) {
        boolean decrypt = !recording.getDestination().toString().endsWith(".TiVo");
        Archivo.logger.info("decrypt = " + decrypt);
        return decrypt;
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

    private void updateProgress(Recording recording, double percent, LocalDateTime startTime, long totalBytesRead, long estimatedLength) {
        Duration elapsedTime = Duration.between(startTime, LocalDateTime.now());
        try {
            double kbs = (totalBytesRead / 1024) / elapsedTime.getSeconds();
            long kbRemaining = (estimatedLength - totalBytesRead) / 1024;
            int secondsRemaining = (int) (kbRemaining / kbs);
            Archivo.logger.info(String.format("Read %d bytes of %d expected bytes (%d%%) in %s (%.1f KB/s)",
                    totalBytesRead, estimatedLength, (int) (percent * 100), elapsedTime, kbs));
            Platform.runLater(() -> recording.statusProperty().setValue(
                    ArchiveStatus.createDownloadingStatus(percent, secondsRemaining)
            ));
        } catch (ArithmeticException e) {
            Archivo.logger.warning("ArithmeticException: " + e.getLocalizedMessage());
        }
    }

    private void verifyDownloadSize(long bytesRead, long bytesExpected) {
        if (bytesRead / (double) bytesExpected < ESTIMATED_SIZE_THRESHOLD) {
            Archivo.logger.severe(String.format("Failed to download file (%,d bytes read, %,d bytes expected)",
                            bytesRead, bytesExpected)
            );
            throw new ArchiveTaskException("Failed to download recording");
        }
    }
}
