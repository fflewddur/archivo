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

package net.straylightlabs.archivo.net;

import net.straylightlabs.archivo.model.Channel;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.RecordingReason;
import net.straylightlabs.archivo.model.RecordingState;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class MindCommandRecordingSearch extends MindCommand {
    private final static JSONArray templateList;

    private final static Logger logger = LoggerFactory.getLogger(MindCommandRecordingSearch.class);

    static {
        templateList = buildTemplate();
    }

    public MindCommandRecordingSearch(String recordingId, String bodyId) {
        super();
        commandType = MindCommandType.RECORDING_SEARCH;
        bodyData.put(RESPONSE_TEMPLATE, templateList);
//        bodyData.put(LOD, LOD_HIGH);
        bodyData.put(RECORDING_ID, recordingId);
        bodyData.put(BODY_ID, bodyId);
    }

    public Recording getRecording() {
        failOnInvalidState();
        Recording.Builder builder = new Recording.Builder();
        logger.info("Response: {}", response);
        if (response.has("recording")) {
            JSONArray recordingsJSON = response.getJSONArray("recording");
            for (Object obj : recordingsJSON) {
                JSONObject recordingJSON = (JSONObject) obj;
                if (recordingJSON.has(RECORDING_ID))
                    builder.recordingId(recordingJSON.getString(RECORDING_ID));
                if (recordingJSON.has(BODY_ID))
                    builder.bodyId(recordingJSON.getString(BODY_ID));
                if (recordingJSON.has("title"))
                    builder.seriesTitle(recordingJSON.getString("title"));
                if (recordingJSON.has("collectionType"))
                    builder.collectionType(recordingJSON.getString("collectionType"));
                if (recordingJSON.has("subtitle"))
                    builder.episodeTitle(recordingJSON.getString("subtitle"));
                if (recordingJSON.has("seasonNumber"))
                    builder.seriesNumber(recordingJSON.getInt("seasonNumber"));
                if (recordingJSON.has("episodeNum"))
                    builder.episodeNumbers(parseEpisodeNumbers(recordingJSON));
                if (recordingJSON.has("duration"))
                    builder.secondsLong(recordingJSON.getInt("duration"));
                if (recordingJSON.has("startTime"))
                    builder.recordedOn(parseUTCDateTime(recordingJSON.getString("startTime")));
                if (recordingJSON.has("description"))
                    builder.description(recordingJSON.getString("description"));
                if (recordingJSON.has("image"))
                    builder.image(parseImages(recordingJSON));
                if (recordingJSON.has("channel"))
                    builder.channel(parseChannel(recordingJSON));
                if (recordingJSON.has("originalAirdate"))
                    builder.originalAirDate(LocalDate.parse(recordingJSON.getString("originalAirdate"),
                            DateTimeFormatter.ofPattern("uuuu-MM-dd")));
                if (recordingJSON.has("state"))
                    builder.state(RecordingState.parse(recordingJSON.getString("state")));
                if (recordingJSON.has("subscriptionIdentifier"))
                    builder.reason(parseReason(recordingJSON.getJSONArray("subscriptionIdentifier")));
                if (recordingJSON.has("drm"))
                    builder.copyable(parseCopyable(recordingJSON.getJSONObject("drm")));
                if (recordingJSON.has("expectedDeletion"))
                    builder.expectedDeletion(parseUTCDateTime(recordingJSON.getString("expectedDeletion")));
            }
        }
        return builder.build();
    }

    private void failOnInvalidState() {
        if (response == null) {
            throw new IllegalStateException("MindCommandRecordingSearch does not have a response.");
        }
    }

    private LocalDateTime parseUTCDateTime(String utcDateTime) {
        ZonedDateTime utc = ZonedDateTime.parse(utcDateTime + " +0000",
                DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss ZZ"));
        return utc.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private List<Integer> parseEpisodeNumbers(JSONObject json) {
        JSONArray episodeNums = json.getJSONArray("episodeNum");
        List<Integer> episodes = new ArrayList<>();
        for (int i = 0; i < episodeNums.length(); i++) {
            episodes.add(episodeNums.getInt(i));
        }
        return episodes;
    }

    private URL parseImages(JSONObject json) {
        URL imageURL = null;
        JSONArray images = json.getJSONArray("image");
        int smallestHeightDiff = Integer.MAX_VALUE;
        for (int i = 0; i < images.length(); i++) {
            JSONObject imageJSON = images.getJSONObject(i);
            if (imageJSON.has("width") && imageJSON.has("height") && imageJSON.has("imageUrl")) {
                int width = imageJSON.getInt("width");
                int height = imageJSON.getInt("height");
                if (width == Recording.DESIRED_IMAGE_WIDTH && height == Recording.DESIRED_IMAGE_HEIGHT) {
                    try {
                        // We found exactly what we wanted, hurray!
                        imageURL = new URL(imageJSON.getString("imageUrl"));
                        break;
                    } catch (MalformedURLException e) {
                        logger.error("Error parsing image URL: ", e);
                    }
                } else {
                    int diff = Math.abs(height - Recording.DESIRED_IMAGE_HEIGHT);
                    if (diff < smallestHeightDiff) {
                        try {
                            // This is the best runner-up image we've found
                            imageURL = new URL(imageJSON.getString("imageUrl"));
                            smallestHeightDiff = diff;
                        } catch (MalformedURLException e) {
                            logger.error("Error parsing image URL: ", e);
                        }
                    }
                }
            }
        }
        return imageURL;
    }

    private Channel parseChannel(JSONObject json) {
        JSONObject channel = json.getJSONObject("channel");
        if (channel.has("channelNumber") && channel.has("name")) {
            URL logoURL = null;
            if (channel.has("logoIndex")) {
                int logoIndex = channel.getInt("logoIndex");
                String logoURLString = String.format("http://%s/ChannelLogo/icon-%d-1.png",
                        client.getAddress().getHostAddress(), logoIndex);
                try {
                    logoURL = new URL(logoURLString);
                } catch (MalformedURLException e) {
                    logger.error("Error building channel logo URL: ", e);
                }
            }
            return new Channel(channel.getString("name"), channel.getString("channelNumber"), logoURL);
        }

        return null;
    }

    private RecordingReason parseReason(JSONArray array) {
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            if (json.has("subscriptionType")) {
                return RecordingReason.parse(json.getString("subscriptionType"));
            }
        }
        return RecordingReason.UNKNOWN;
    }

    private boolean parseCopyable(JSONObject json) {
        return (json.has("cgms") && json.getString("cgms").equalsIgnoreCase("copyFreely"));
    }

    private static JSONArray buildTemplate() {
        JSONArray templates = new JSONArray();
        JSONObject template;

        // Tell the recordingList to only include recording objects
        template = new JSONObject();
        template.put(TYPE, RESPONSE_TEMPLATE);
        template.put(FIELD_NAME, Collections.singletonList("recording"));
        template.put(TYPE_NAME, "recordingList");
        templates.put(template);

        // Tell the recording object to only include info we need for our UI
        template = new JSONObject();
        template.put(TYPE, RESPONSE_TEMPLATE);
        template.put(FIELD_NAME, Arrays.asList("recordingId", "channel", "originalAirdate", "state", "subtitle",
                "startTime", "requestedEndTime", "episodeNum", "description", "title", "duration", "seasonNumber",
                "image", "drm", "subscriptionIdentifier", "bodyId", "expectedDeletion"));
        template.put(TYPE_NAME, "recording");
        templates.put(template);

        // Only get useful channel information
        template = new JSONObject();
        template.put(TYPE, RESPONSE_TEMPLATE);
        template.put(FIELD_NAME, Arrays.asList("channelNumber", "name", "logoIndex"));
        template.put(TYPE_NAME, "channel");
        templates.put(template);

        // Only get useful DRM information
        template = new JSONObject();
        template.put(TYPE, RESPONSE_TEMPLATE);
        template.put(FIELD_NAME, Collections.singletonList("cgms"));
        template.put(TYPE_NAME, "drm");
        templates.put(template);

        // Only get useful subscription information
        template = new JSONObject();
        template.put(TYPE, RESPONSE_TEMPLATE);
        template.put(FIELD_NAME, Collections.singletonList("subscriptionType"));
        template.put(TYPE_NAME, "subscriptionIdentifier");
        templates.put(template);

        return templates;
    }
}
