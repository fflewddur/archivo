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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FFprobeOutputReader extends ProcessOutputReader {
    private final List<String> lines;
    private String streamOutput;
    private double videoStartTime;
    private double audioStartTime;
    private double audioOffset;

    private final static Pattern STREAM_VIDEO = Pattern.compile("\\[STREAM].*?codec_type=video.*?start_time=([\\d\\.]+).*?\\[/STREAM]");
    private final static Pattern STREAM_AUDIO = Pattern.compile("\\[STREAM].*?codec_type=audio.*?start_time=([\\d\\.]+).*?\\[/STREAM]");

    public FFprobeOutputReader(Recording recording) {
        super(recording);
        lines = new ArrayList<>();
    }

    @Override
    public void processLine(String line) {
        lines.add(line);
    }

    /**
     * Use the information from FFprobe to determine the offset (in seconds) between the first video stream and
     * the first audio stream.
     *
     * @return Number of seconds the audio is ahead of the video, or 0 if the audio is behind the video.
     */
    public double getAudioOffset() {
        if (streamOutput == null) {
            findAudioOffset();
        }
        return audioOffset;
    }

    private void findAudioOffset() {
        streamOutput = lines.stream().collect(Collectors.joining(" "));
        Archivo.logger.debug("Stream output: {}", streamOutput);
        if (!parseFirstVideoStartTime()) {
            // No video
            Archivo.logger.debug("No video stream found");
            audioOffset = 0;
        } else if (!parseFirstAudioStartTime()) {
            // No audio
            Archivo.logger.debug("No audio stream found");
            audioOffset = 0;
        } else {
            audioOffset = Math.max(videoStartTime - audioStartTime, 0);
        }
    }

    private boolean parseFirstVideoStartTime() {
        Matcher matcher = STREAM_VIDEO.matcher(streamOutput);
        if (matcher.find()) {
            videoStartTime = Double.parseDouble(matcher.group(1));
            Archivo.logger.debug("Video start time: {}", videoStartTime);
            return true;
        }
        return false;
    }

    private boolean parseFirstAudioStartTime() {
        Matcher matcher = STREAM_AUDIO.matcher(streamOutput);
        if (matcher.find()) {
            audioStartTime = Double.parseDouble(matcher.group(1));
            Archivo.logger.debug("Audio start time: {}", audioStartTime);
            return true;
        }
        return false;
    }
}
