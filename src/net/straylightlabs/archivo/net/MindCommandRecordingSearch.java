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

package net.straylightlabs.archivo.net;

import net.straylightlabs.archivo.model.Recording;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

class MindCommandRecordingSearch extends MindCommand {
    private final static JSONArray templateList;

    static {
        templateList = buildTemplate();
    }

    public MindCommandRecordingSearch(String recordingId, String bodyId) {
        super();
        commandType = MindCommandType.RECORDING_SEARCH;
        bodyData.put("responseTemplate", templateList);
        bodyData.put("recordingId", recordingId);
        bodyData.put("bodyId", bodyId);
    }

    public Recording getRecording() {
        failOnInvalidState();
        Recording.Builder builder = new Recording.Builder();
        System.out.println("Response: " + response);
        if (response.has("recording")) {
            JSONArray recordingsJSON = response.getJSONArray("recording");
            for (Object obj : recordingsJSON) {
                JSONObject recordingJSON = (JSONObject) obj;
                if (recordingJSON.has("title"))
                    builder.seriesTitle(recordingJSON.getString("title"));
                if (recordingJSON.has("subtitle"))
                    builder.episodeTitle(recordingJSON.getString("subtitle"));
                if (recordingJSON.has("seasonNumber"))
                    builder.episodeNumber(recordingJSON.getInt("seasonNumber"));
//                if (recordingJSON.has("episodeNum"))
//                    builder.episodeNumber(Integer.parseInt(recordingJSON.getString("episodeNum")));
                if (recordingJSON.has("duration"))
                    builder.minutesLong(recordingJSON.getInt("duration") / 60);
                if (recordingJSON.has("startTime")) {
                    builder.recordedOn(LocalDateTime.parse(recordingJSON.getString("startTime"),
                            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")));
                }
            }
        }
        return builder.build();
    }

    private void failOnInvalidState() {
        if (response == null) {
            throw new IllegalStateException("MindCommandRecordingSearch does not have a response.");
        }
    }

    private static JSONArray buildTemplate() {
        JSONArray templates = new JSONArray();
        JSONObject template;

        // Only get the recording
        template = new JSONObject();
        template.put("type", "responseTemplate");
        template.put("fieldName", Collections.singletonList("recording"));
        template.put("typeName", "recordingList");
        templates.put(template);

        // Get the channel
        template = new JSONObject();
        template.put("type", "responseTemplate");
        template.put("fieldName", Arrays.asList("channel", "originalAirdate", "state", "subtitle",
                "startTime", "episodeNum", "description", "title", "duration", "seasonNumber"));
        template.put("typeName", "recording");
        templates.put(template);

        // Only get useful channel information
        template = new JSONObject();
        template.put("type", "responseTemplate");
        template.put("fieldName", Arrays.asList("channelNumber", "name"));
        template.put("typeName", "channel");
        templates.put(template);

        return templates;
    }
}
