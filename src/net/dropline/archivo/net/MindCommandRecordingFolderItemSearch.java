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

package net.dropline.archivo.net;

import net.dropline.archivo.model.Recording;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MindCommandRecordingFolderItemSearch extends MindCommand {
    private List<Recording> recordings;

    private final static JSONArray templateList;

    static {
        templateList = buildTemplate();
    }

    public MindCommandRecordingFolderItemSearch() {
        super();
        this.commandType = MindCommandType.RECORDING_FOLDER_ITEM_SEARCH;
        bodyData.put("responseTemplate", templateList);
        bodyData.put("bodyId", "-");
        // Get all of the recordings, don't group by title
        bodyData.put("flatten", "true");
        bodyData.put("noLimit", "true");
    }

    /**
     * Parse the response to fill @recordings.
     */
    @Override
    protected void afterExecute() {
        System.out.println("Folder = " + response);
        JSONArray items = response.getJSONArray("recordingFolderItem");
        recordings = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.getJSONObject(i);
            String bodyId = o.getString("bodyId");
            String recordingId = o.getString("childRecordingId");
//            System.out.println(o);
            MindCommandRecordingSearch command = new MindCommandRecordingSearch(recordingId, bodyId);
            try {
//                System.out.println("executing command...");
                command.executeOn(this.client);
                recordings.add(command.getRecording());
//                System.out.println("recording id result: " + command.response);
            } catch (IOException e) {
                System.err.println("Error: " + e.getLocalizedMessage());
            }

//            recordings.add(new Recording.Builder().seriesTitle(o.getString("title")).build());
        }
    }

    /**
     * Parse the JSON response to a list of Recordings.
     *
     * @return List of Recording objects currently stored on the selected TiVo.
     */
    public List<Recording> getRecordings() {
        return recordings;
    }

    private static JSONArray buildTemplate() {
        JSONArray templates = new JSONArray();
        JSONObject template;

        // Only get the recording ID
        template = new JSONObject();
        template.put("type", "responseTemplate");
        template.put("fieldName", Arrays.asList("childRecordingId"));
        template.put("typeName", "recordingFolderItem");
        templates.put(template);

        return templates;
    }
}
