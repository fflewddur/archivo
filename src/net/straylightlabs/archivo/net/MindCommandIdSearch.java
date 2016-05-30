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

import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.Tivo;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Get the ObjectId associated with a given recording, and use it to build an HTTP URL to access the recording's
 * transport stream file.
 */
public class MindCommandIdSearch extends MindCommand {
    private final Recording recording;
    private final Tivo tivo;
    private boolean compatibilityMode;

    private final static Logger logger = LoggerFactory.getLogger(MindCommandIdSearch.class);

    public MindCommandIdSearch(Recording recording, Tivo tivo) {
        super();
        this.commandType = MindCommandType.ID_SEARCH;
        this.recording = recording;
        this.tivo = tivo;
        bodyData.put(OBJECT_ID, recording.getRecordingId());
        bodyData.put(BODY_ID, recording.getBodyId());
        bodyData.put(NAMESPACE, "mfs");
    }

    /**
     * Turn on compatibility mode, which downloads files in the older and slower PS format.
     */
    @SuppressWarnings("unused")
    public void setCompatibilityMode(boolean val) {
        compatibilityMode = val;
    }

    /**
     * Build a URL for downloading the video file associated with this recording.
     */
    public URL getDownloadUrl() {
        failOnInvalidState();
        try {
            if (response.has(OBJECT_ID)) {
                JSONArray ids = response.getJSONArray(OBJECT_ID);
                String id = ids.getString(0).replaceFirst("mfs:rc\\.", "");
                String title = URLEncoder.encode(recording.getSeriesTitle(), RPC_ENCODING);
                String tsFormat = "&Format=video/x-tivo-mpeg-ts";
                if (compatibilityMode) {
                    tsFormat = "";
                }
                String url = String.format(
                        "http://%s/download/%s.TiVo?Container=%%2FNowPlaying&id=%s%s",
                        tivo.getClient().getAddress().getHostAddress(), title, id, tsFormat
                );
                return new URL(url);
            }
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            logger.error("Error building download URL: ", e);
        }
        throw new IllegalStateException("No URL for recording");
    }

    private void failOnInvalidState() {
        if (response == null) {
            throw new IllegalStateException("MindCommandIdSearch does not have a response.");
        }
    }
}
