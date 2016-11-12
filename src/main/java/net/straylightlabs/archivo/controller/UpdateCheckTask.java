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

import javafx.concurrent.Task;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.SoftwareUpdateDetails;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Check for software updates and notify the user if one is available.
 */
public class UpdateCheckTask extends Task<SoftwareUpdateDetails> {
    public final static Logger logger = LoggerFactory.getLogger(UpdateCheckTask.class);

    @Override
    protected SoftwareUpdateDetails call() throws Exception {
        CloseableHttpClient httpclient = buildHttpClient();
        URIBuilder builder = new URIBuilder();
        builder.setScheme("https").setHost("straylightlabs.net").setPath("/archivo/check_for_updates.php")
                .addParameter("major_ver", Integer.toString(Archivo.APP_MAJOR_VERSION))
                .addParameter("minor_ver", Integer.toString(Archivo.APP_MINOR_VERSION))
                .addParameter("release_ver", Integer.toString(Archivo.APP_RELEASE_VERSION))
                .addParameter("is_beta", Boolean.toString(Archivo.IS_BETA))
                .addParameter("beta_ver", Integer.toString(Archivo.BETA_VERSION));
        URI updateURI = builder.build();
        logger.debug("Fetching {}...", updateURI);
        HttpGet httpGet = new HttpGet(updateURI);
        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (statusCode == 200) {
                logger.info("Successfully fetched update URL");
                JSONObject json = new JSONObject(EntityUtils.toString(entity));
                return getCurrentRelease(json);
            } else {
                EntityUtils.consume(entity);
                logger.error("Error fetching release list: HTTP code {}", statusCode);
                throw new IOException(String.format("Error fetching %s: status code %d", updateURI, statusCode));
            }
        }
    }

    private CloseableHttpClient buildHttpClient() {
        return HttpClients.custom()
                .useSystemProperties()
                .setUserAgent(Archivo.USER_AGENT)
                .build();
    }

    private SoftwareUpdateDetails getCurrentRelease(JSONObject json) {
        logger.debug("Release JSON: {}", json);
        try {
            if (json.getBoolean("update_available")) {
                URL location = new URL(json.getString("location"));
                List<String> changes = parseChangeList(json);
                LocalDate date = LocalDate.parse(json.getString("date"));
                return new SoftwareUpdateDetails(
                        SoftwareUpdateDetails.versionToString(
                                json.getInt("major_ver"), json.getInt("minor_ver"), json.getInt("release_ver"),
                                json.getBoolean("is_beta"), json.getInt("beta_ver")
                        ), location, date, changes
                );
            } else {
                return SoftwareUpdateDetails.UNAVAILABLE;
            }
        } catch (MalformedURLException e) {
            logger.error("Error parsing update location: {}", e.getLocalizedMessage());
            return SoftwareUpdateDetails.UNAVAILABLE;
        }
    }

    private List<String> parseChangeList(JSONObject json) {
        List<String> changes = new ArrayList<>();
        JSONArray changeArray = json.getJSONArray("changes");
        for (int i = 0; i < changeArray.length(); i++) {
            changes.add(changeArray.get(i).toString());
        }
        return changes;
    }
}
