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
import java.util.List;

public class MindCommandRecordingFolderItemSearch extends MindCommand {
    public MindCommandRecordingFolderItemSearch() {
        super();
        this.commandType = MindCommandType.RECORDING_FOLDER_ITEM_SEARCH;
        this.bodyData.put("bodyId", "-");
        this.bodyData.put("flatten", "true");
        this.bodyData.put("noLimit", "true");
        JSONObject template = new JSONObject("{\"type\":\"responseTemplate\",\"fieldName\":[\"title\", \"childRecordingId\"],\"typeName\":\"recordingFolderItem\"}");
        this.bodyData.put("responseTemplate", template);
    }

    @Override
    protected void afterExecute() {
        System.out.println("afterExecute()");
        System.out.println("response = " + response);
        JSONArray items = response.getJSONArray("recordingFolderItem");
//        List<Recording> recordings = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.getJSONObject(i);
            String bodyId = o.getString("bodyId");
            String recordingId = o.getString("childRecordingId");
            System.out.println(o);
            MindCommand command = new MindCommandRecordingSearch(recordingId);
            command.bodyData.put("bodyId", bodyId);
            try {
                System.out.println("executing command...");
                command.executeOn(this.client);
                System.out.println("recording id result: " + command.response);
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
        System.out.println("Entering getRecordings()...");
        JSONArray items = response.getJSONArray("recordingFolderItem");
        List<Recording> recordings = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.getJSONObject(i);
            recordings.add(new Recording.Builder().seriesTitle(o.getString("title")).build());
        }

        return recordings;
    }
}
