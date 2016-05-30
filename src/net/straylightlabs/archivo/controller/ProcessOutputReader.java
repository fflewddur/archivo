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
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

abstract class ProcessOutputReader implements Runnable {
    final Recording recording;
    private final Set<Integer> exitCodes;
    private final LocalDateTime startTime;
    private final Deque<Integer> recentEndTimeEstimates;
    private double priorProgress;
    private InputStream inputStream;
    private final StringBuilder output;

    private final static int MIN_END_TIME_ESTIMATES = 5;
    private final static int MAX_END_TIME_ESTIMATES = 10;

    private final static Logger logger = LoggerFactory.getLogger(ProcessOutputReader.class);

    ProcessOutputReader(Recording recording) {
        this.recording = recording;
        exitCodes = new HashSet<>();
        exitCodes.add(0);
        startTime = LocalDateTime.now();
        recentEndTimeEstimates = new ArrayDeque<>();
        priorProgress = -1;
        output = new StringBuilder();
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void addExitCode(int code) {
        exitCodes.add(code);
    }

    public boolean isValidExitCode(int code) {
        return exitCodes.contains(code);
    }

    public String getOutput() {
        return output.toString();
    }

    void addLineToOutput(String line) {
        output.append(line);
        output.append(System.lineSeparator());
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (Thread.interrupted()) {
                    logger.info("ProcessOutputReader interrupted");
                    return;
                }
                processLine(line);
            }
        } catch (IOException e) {
            logger.error("IOException in ProcessOutputReader: ", e);
        }
    }

    protected abstract void processLine(String line);

    int getSecondsRemaining(double progress) {
        if (progressRegressed(progress)) {
            clearEstimates();
        }
        int secondsRemaining = calcSecondsRemainingFromProgress(progress);
        updateEstimates(secondsRemaining);
        if (recentEndTimeEstimates.size() < MIN_END_TIME_ESTIMATES) {
            return ArchiveStatus.TIME_UNKNOWN;
        } else {
            return estimateSecondsRemaining();
        }
    }

    private boolean progressRegressed(double progress) {
        boolean progressRegressed = false;
        if (progress < priorProgress) {
            progressRegressed = true;
        }
        priorProgress = progress;
        return progressRegressed;
    }

    private void clearEstimates() {
        recentEndTimeEstimates.clear();
    }

    private int calcSecondsRemainingFromProgress(double progress) {
        if (progress > 0) {
            if (progress > 1) {
                progress = .99;
            }
            long elapsedSeconds = Duration.between(startTime, LocalDateTime.now()).getSeconds();
            double progressPerSecond = progress / elapsedSeconds;
            return (int) ((1.0 - progress) / progressPerSecond);
//            logger.info("Elapsed seconds = {}, progressPerSecond = {}, secondsRemaining", elapsedSeconds, progressPerSecond);
        } else {
            logger.warn("Progress <= 0");
            return ArchiveStatus.TIME_UNKNOWN;
        }
    }

    private void updateEstimates(int secondsRemaining) {
//        Archivo.logger.debug("Adding {} to estimates", secondsRemaining);
        recentEndTimeEstimates.addLast(secondsRemaining);
        if (recentEndTimeEstimates.size() > MAX_END_TIME_ESTIMATES) {
            recentEndTimeEstimates.removeFirst();
        }
    }

    /**
     * Average the time estimates in our queue.
     */
    private int estimateSecondsRemaining() {
        int sum = recentEndTimeEstimates.stream().mapToInt(i -> i).sum();
//        Archivo.logger.debug("Sum = {}, size = {}", sum, recentEndTimeEstimates.size());
        return sum / recentEndTimeEstimates.size();
    }
}
