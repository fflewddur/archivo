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
import net.straylightlabs.archivo.model.Tivo;
import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by todd on 8/24/15.
 */
public class MindCommandIdSearch extends MindCommand {
    private final Recording recording;
    private final Tivo tivo;

    public MindCommandIdSearch(Recording recording, Tivo tivo) {
        super();
        this.commandType = MindCommandType.ID_SEARCH;
        this.recording = recording;
        this.tivo = tivo;
        bodyData.put("objectId", recording.getRecordingId());
        bodyData.put("bodyId", recording.getBodyId());
        bodyData.put("namespace", "mfs");
    }

    /**
     * Build a URL for downloading the video file associated with this recording.
     */
    public URL getDownloadUrl() {
        failOnInvalidState();
        try {
            if (response.has("objectId")) {
                JSONArray ids = response.getJSONArray("objectId");
                String id = ids.getString(0).replaceFirst("mfs:rc\\.", "");
                String title = URLEncoder.encode(recording.getSeriesTitle(), RPC_ENCODING);
                StringBuilder sb = new StringBuilder();
                sb.append("http://");
                sb.append(tivo.getClient().getAddress().toString().replaceAll("/", ""));
                sb.append("/download/");
                sb.append(title);
                sb.append(".TiVo?Container=%2FNowPlaying&id=");
                sb.append(id);
                sb.append("&Format=video/x-tivo-mpeg-ts");
                return new URL(sb.toString());
            }
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            Archivo.logger.severe("Error building download URL: " + e.getLocalizedMessage());
        }
        throw new IllegalStateException("No URL for recording");
    }

    private void failOnInvalidState() {
        if (response == null) {
            throw new IllegalStateException("MindCommandIdSearch does not have a response.");
        }
    }
}
