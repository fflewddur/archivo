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

package net.straylightlabs.archivo.controller;

import javafx.concurrent.Task;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.SoftwareUpdateDetails;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Check for software updates and notify the user if one is available.
 */
public class UpdateCheckTask extends Task<SoftwareUpdateDetails> {
    private Document releaseDocument;
    private String latestReleaseVersion;
    private List<SoftwareUpdateDetails> releaseList;

    private static final String UPDATE_CHECK_URL = "http://straylightlabs.net/archivo/current_version.xml";

    @Override
    protected SoftwareUpdateDetails call() throws Exception {
        CloseableHttpClient httpclient = buildHttpClient();
        HttpGet httpGet = new HttpGet(UPDATE_CHECK_URL);
        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (statusCode == 200) {
                Archivo.logger.info("Successfully fetched current_version.xml");
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                try {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    releaseDocument = builder.parse(entity.getContent());
                    parseReleaseList();
                    return getCurrentRelease();
                } catch (ParserConfigurationException | IllegalStateException | SAXException e) {
                    Archivo.logger.error("Error parsing release list: {}", e.getLocalizedMessage());
                    throw e;
                }
            } else {
                EntityUtils.consume(entity);
                Archivo.logger.error("Error fetching release list: HTTP code {}", statusCode);
                throw new IOException(String.format("Error fetching %s: status code %d", UPDATE_CHECK_URL, statusCode));
            }
        }
    }

    private CloseableHttpClient buildHttpClient() {
        return HttpClients.custom()
                .useSystemProperties()
                .setUserAgent(Archivo.USER_AGENT)
                .build();
    }

    private void parseReleaseList() throws IOException {
        releaseList = new ArrayList<>();
        NodeList nodes = releaseDocument.getElementsByTagName("App");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.hasAttributes() && Archivo.APPLICATION_NAME.equals(node.getAttributes().getNamedItem("name").getNodeValue())) {
                NodeList appNodes = node.getChildNodes();
                for (int j = 0; j < appNodes.getLength(); j++) {
                    Node appNode = appNodes.item(j);
                    if (appNode.getNodeName().equals("CurrentRelease")) {
                        latestReleaseVersion = appNode.getTextContent();
                    } else if (appNode.getNodeName().equals("ReleaseList")) {
                        NodeList releaseNodes = appNode.getChildNodes();
                        for (int k = 0; k < releaseNodes.getLength(); k++) {
                            Node releaseNode = releaseNodes.item(k);
                            if (releaseNode.getNodeName().equals("Release")) {
                                parseRelease(releaseNode);
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseRelease(Node releaseNode) throws IOException {
        NamedNodeMap attributes = releaseNode.getAttributes();
        String version = attributes.getNamedItem("version").getTextContent();
        URL location = new URL(attributes.getNamedItem("url").getTextContent());
        LocalDate releaseDate = LocalDate.parse(attributes.getNamedItem("date").getTextContent());
        List<String> changes = new ArrayList<>();
        NodeList children = releaseNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("Change")) {
                changes.add(child.getTextContent());
            }
        }

        SoftwareUpdateDetails details = new SoftwareUpdateDetails(version, location, releaseDate, changes);
        releaseList.add(details);
    }

    private SoftwareUpdateDetails getCurrentRelease() {
        Archivo.logger.debug("Release list: {}", releaseList);

        // Figure out the dates associated with the user's release and the current release
        SoftwareUpdateDetails latestRelease = SoftwareUpdateDetails.UNAVAILABLE;
        SoftwareUpdateDetails usersRelease = SoftwareUpdateDetails.UNAVAILABLE;
        for (SoftwareUpdateDetails release : releaseList) {
            if (release.getVersion().equals(latestReleaseVersion)) {
                latestRelease = release;
            } else if (release.getVersion().equals(Archivo.APPLICATION_VERSION)) {
                usersRelease = release;
            }
        }

        Archivo.logger.debug("latestRelease = {}, usersRelease = {}", latestRelease, usersRelease);
        if (latestRelease == usersRelease) {
            return SoftwareUpdateDetails.UNAVAILABLE;
        } else if (usersRelease != SoftwareUpdateDetails.UNAVAILABLE) {
            // Build a summary of all changes occurring between the user's release and the latest release
            List<String> changes = new ArrayList<>();
            for (SoftwareUpdateDetails release : releaseList) {
                if (release.getReleaseDate().isAfter(usersRelease.getReleaseDate())) {
                    changes.addAll(release.getChanges());
                }
            }
            return new SoftwareUpdateDetails(latestRelease.getVersion(), latestRelease.getLocation(),
                    latestRelease.getReleaseDate(), changes);
        } else {
            return latestRelease;
        }
    }
}
