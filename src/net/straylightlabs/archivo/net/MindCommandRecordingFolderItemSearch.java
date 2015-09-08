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

import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.Series;
import net.straylightlabs.archivo.model.Tivo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class MindCommandRecordingFolderItemSearch extends MindCommand {
    private List<Series> series;
    private Tivo tivo; // Since we always call this command at startup, use it to figure out the TiVo's bodyId

    private final static JSONArray templateList;

    static {
        templateList = buildTemplate();
    }

    public MindCommandRecordingFolderItemSearch(Tivo tivo) {
        super();
        this.commandType = MindCommandType.RECORDING_FOLDER_ITEM_SEARCH;
        this.tivo = tivo;
        bodyData.put("responseTemplate", templateList);
        bodyData.put("bodyId", "-");
        // Get all of the recordings, don't group by title
        bodyData.put("flatten", "true");
        bodyData.put("noLimit", "true");
    }

    /**
     * Parse the response to fill @seriesToRecordings.
     */
    @Override
    protected void afterExecute() {
        JSONArray items = response.getJSONArray("recordingFolderItem");
        Map<String, List<Recording>> seriesToRecordings = mapSeriesToRecordings(items);
        series = buildSeriesList(seriesToRecordings);
    }

    private Map<String, List<Recording>> mapSeriesToRecordings(JSONArray items) {
        Map<String, List<Recording>> seriesToRecordings = new HashMap<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.getJSONObject(i);
            String bodyId = o.getString("bodyId");
            tivo.setBodyId(bodyId);
            String recordingId = o.getString("childRecordingId");
            MindCommandRecordingSearch command = new MindCommandRecordingSearch(recordingId, bodyId);
            try {
                command.executeOn(this.client);
                Recording recording = command.getRecording();
                String seriesTitle = recording.getSeriesTitle();
                if (seriesToRecordings.containsKey(seriesTitle)) {
                    seriesToRecordings.get(seriesTitle).add(recording);
                } else {
                    List<Recording> recordings = new ArrayList<>();
                    recordings.add(recording);
                    seriesToRecordings.put(seriesTitle, recordings);
                }
            } catch (IOException e) {
                Archivo.logger.severe("Error: " + e.getLocalizedMessage());
            }
        }
        return seriesToRecordings;
    }

    private List<Series> buildSeriesList(Map<String, List<Recording>> seriesToRecordings) {
        List<Series> series = new ArrayList<>();
        seriesToRecordings.forEach((title, recordings) -> series.add(new Series(title, recordings)));
        return series;
    }

    public List<Series> getSeries() {
        if (series == null) {
            throw new IllegalStateException("Cannot call getSeries() before series has been initialized");
        }
        return series;
    }

    private static JSONArray buildTemplate() {
        JSONArray templates = new JSONArray();
        JSONObject template;

        // Only get the recording ID
        template = new JSONObject();
        template.put("type", "responseTemplate");
        template.put("fieldName", Collections.singletonList("childRecordingId"));
        template.put("typeName", "recordingFolderItem");
        templates.put(template);

        return templates;
    }
}
