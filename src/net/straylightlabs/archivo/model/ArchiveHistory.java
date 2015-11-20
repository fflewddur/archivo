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

package net.straylightlabs.archivo.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Store a record of each successful archive task, so we can show the user which
 * recordings they've already archived.
 */
public class ArchiveHistory {
    private final Path location;
    private final Map<String, ArchiveHistoryItem> items;

    private final static String ATT_ID = "id";
    private final static String ATT_DATE = "date";
    private final static String ATT_PATH = "path";

    private final static Logger logger = LoggerFactory.getLogger(ArchiveHistory.class);
    private final static DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    public static ArchiveHistory loadFrom(Path location) {
        ArchiveHistory ah = new ArchiveHistory(location);
        if (ah.exists()) {
            ah.load();
        }
        return ah;
    }

    private ArchiveHistory(Path location) {
        this.location = location;
        this.items = new HashMap<>();
    }

    private boolean exists() {
        return Files.isRegularFile(location);
    }

    private void load() {
        logger.info("Loading archive history from {}", location);
        try (InputStream historyReader = Files.newInputStream(location)) {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document doc = builder.parse(historyReader);
            NodeList itemList = doc.getElementsByTagName("Item");
            for (int i = 0; i < itemList.getLength(); i++) {
                Node item = itemList.item(i);
                NamedNodeMap attributes = item.getAttributes();
                String id = attributes.getNamedItem(ATT_ID).getTextContent();
                LocalDate date = LocalDate.parse(attributes.getNamedItem(ATT_DATE).getTextContent());
                Path location = Paths.get(attributes.getNamedItem(ATT_PATH).getTextContent());
                ArchiveHistoryItem historyItem = new ArchiveHistoryItem(id, date, location);
                items.put(id, historyItem);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.error("Error loading archive history: ", e);
        }
    }

    public void save() {
        logger.info("Saving archive history to {}", location);
        try (BufferedWriter historyWriter = Files.newBufferedWriter(location)) {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("HistoryItems");
            doc.appendChild(root);
            items.entrySet().forEach(entry -> {
                ArchiveHistoryItem item = entry.getValue();
                Element element = doc.createElement("Item");
                element.setAttribute(ATT_ID, item.getRecordingId());
                element.setAttribute(ATT_DATE, item.getDateArchived().toString());
                element.setAttribute(ATT_PATH, item.getLocation().toString());
                root.appendChild(element);
            });
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult historyFile = new StreamResult(historyWriter);
            transformer.transform(source, historyFile);
        } catch (ParserConfigurationException | TransformerException | IOException e) {
            logger.error("Error saving archive history: ", e);
        }
    }

    public boolean contains(Recording recording) {
        verifyRecordingIsValid(recording);

        return items.containsKey(recording.getRecordingId());
    }

    private void verifyRecordingIsValid(Recording recording) {
        if (recording == null) {
            throw new IllegalArgumentException("Recording can't be null");
        }
    }

    public ArchiveHistoryItem get(Recording recording) {
        verifyRecordingIsValid(recording);

        ArchiveHistoryItem item = items.get(recording.getRecordingId());
        if (item == null) {
            throw new IllegalArgumentException(
                    String.format("No history exists for recording '%s'", recording.getRecordingId())
            );
        }

        return item;
    }

    public void add(Recording recording) {
        verifyRecordingIsValid(recording);

        ArchiveHistoryItem historyItem = new ArchiveHistoryItem(
                recording.getRecordingId(), LocalDate.now(), recording.getDestination()
        );
        items.put(historyItem.getRecordingId(), historyItem);
    }

    public static class ArchiveHistoryItem {
        private final LocalDate dateArchived;
        private final Path location;
        private final String recordingId;

        public ArchiveHistoryItem(String recordingId, LocalDate dateArchived, Path location) {
            this.recordingId = recordingId;
            this.dateArchived = dateArchived;
            this.location = location;
        }

        public String getRecordingId() {
            return recordingId;
        }

        public Path getLocation() {
            return location;
        }

        public LocalDate getDateArchived() {
            return dateArchived;
        }
    }
}
