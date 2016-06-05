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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class CrashReportTask extends Task<Void> {
    private final String userId;
    private final Path logPath;

    private final static String CRASH_REPORT_URL = "http://straylightlabs.net/archivo/crash_report.php";
    private final static Logger logger = LoggerFactory.getLogger(CrashReportTask.class);

    public CrashReportTask(String userId, Path logPath) {
        this.userId = userId;
        this.logPath = logPath;
    }

    @Override
    protected Void call() {
        performUpload();
        return null;
    }

    private void performUpload() {
        try (CloseableHttpClient client = buildHttpClient()) {
            HttpPost post = new HttpPost(CRASH_REPORT_URL);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            FileBody logFile = new FileBody(logPath.toFile(), ContentType.TEXT_PLAIN);
            builder.addPart("log", logFile);
            StringBody uid = new StringBody(userId, ContentType.TEXT_PLAIN);
            builder.addPart("uid", uid);
            HttpEntity postEntity = builder.build();
            post.setEntity(postEntity);
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() != 200) {
                logger.debug("Error uploading crash report: {}", response.getStatusLine());
//            } else {
//                HttpEntity responseEntity = response.getEntity();
//                if (responseEntity != null) {
//                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseEntity.getContent()))) {
//                        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
//                            logger.debug("Upload response: {}", line);
//                        }
//                    }
//                }
            }
        } catch (IOException e) {
            logger.error("Error upload crash report: {}", e.getLocalizedMessage());
        }
    }

    private CloseableHttpClient buildHttpClient() {
        return HttpClients.custom()
                .useSystemProperties()
                .setUserAgent(Archivo.USER_AGENT)
                .build();
    }
}
