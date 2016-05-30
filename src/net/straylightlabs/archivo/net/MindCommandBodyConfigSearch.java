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

import net.straylightlabs.archivo.model.Tivo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

public class MindCommandBodyConfigSearch extends MindCommand {
    private final Tivo tivo;

    private final static JSONArray templateList;

    private final static Logger logger = LoggerFactory.getLogger(MindCommandBodyConfigSearch.class);

    static {
        templateList = buildTemplate();
    }

    public MindCommandBodyConfigSearch(Tivo tivo) {
        super();
        this.commandType = MindCommandType.BODY_CONFIG_SEARCH;
        this.tivo = tivo;
        bodyData.put(RESPONSE_TEMPLATE, templateList);
        bodyData.put(BODY_ID, tivo.getBodyId());
        bodyData.put(LOD, LOD_HIGH);
    }

    @Override
    protected void afterExecute() {
        logger.info("Response: {}", response.toString());
        JSONArray configs = response.getJSONArray("bodyConfig");
        // There should only be one config in the array
        JSONObject config = configs.getJSONObject(0);
        if (config.has("userDiskUsed")) {
            tivo.setStorageBytesUsed(Long.parseLong(config.getString("userDiskUsed")));
        }
        if (config.has("userDiskSize")) {
            tivo.setStorageBytesTotal(Long.parseLong(config.getString("userDiskSize")));
        }
    }

    private static JSONArray buildTemplate() {
        JSONArray templates = new JSONArray();
        JSONObject template;

        // Only get the bodyConfig
        template = new JSONObject();
        template.put(TYPE, "responseTemplate");
        template.put(FIELD_NAME, Collections.singletonList("bodyConfig"));
        template.put(TYPE_NAME, "bodyConfigList");
        templates.put(template);

        // Only get the disk information
        template = new JSONObject();
        template.put(TYPE, "responseTemplate");
        template.put(FIELD_NAME, Arrays.asList("userDiskUsed", "userDiskSize"));
        template.put(TYPE_NAME, "bodyConfig");
        templates.put(template);

        return templates;
    }
}
