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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Base class for all TiVo RPC commands.
 */

public abstract class MindCommand {
    MindRPC client;
    MindCommandType commandType;
    final JSONObject bodyData;
    JSONObject response;

    static final String RPC_ENCODING = "UTF-8";

    // We use these values in the subclasses
    static final String BODY_ID = "bodyId";
    static final String FIELD_NAME = "fieldName";
    static final String LOD = "levelOfDetail";
    static final String LOD_HIGH = "high";
    static final String NAMESPACE = "namespace";
    static final String OBJECT_ID = "objectId";
    static final String RECORDING_ID = "recordingId";
    static final String RESPONSE_TEMPLATE = "responseTemplate";
    static final String STATE = "state";
    static final String STATE_DELETE = "deleted";
    static final String TYPE = "type";
    static final String TYPE_NAME = "typeName";

    private final static Logger logger = LoggerFactory.getLogger(MindCommand.class);

    MindCommand() {
        this.commandType = MindCommandType.UNKNOWN;
        this.bodyData = new JSONObject();
    }

    /**
     * Copy constructor to make it easy to send additional commands based off of one template.
     * Note that the @response field will not be copied.
     *
     * @param source The MindCommand to copy.
     */
    @SuppressWarnings("unused")
    protected MindCommand(MindCommand source) {
        this.client = source.client;
        this.commandType = source.commandType;
        this.bodyData = source.bodyData;
        this.response = null;
    }

    private JSONObject execute() throws IOException {
        assert (client != null);
        response = this.client.send(buildRequest());
        failOnInvalidResponse();
        afterExecute();
        return response;
    }

    public final JSONObject executeOn(MindRPC client) throws IOException {
        this.client = client;
        return execute();
    }

    /**
     * Override this method to execute code following the network operation.
     */
    void afterExecute() {

    }

    private String buildRequest() {
        failOnInvalidCommand();

        List<String> headerLines = buildHeaderLines();

        bodyData.put("type", commandType.toString());
        String body = bodyData.toString();

        // The length is the number of characters plus two bytes for each \r\n line ending
        int headerLength = headerLines.stream().mapToInt(String::length).sum() + (headerLines.size() * MindRPC.LINE_ENDING.length());
        headerLines.add(0, String.format("MRPC/2 %d %d", headerLength, body.length()));

        StringJoiner joiner = new StringJoiner(MindRPC.LINE_ENDING, "", MindRPC.LINE_ENDING);
        headerLines.stream().forEach(joiner::add);
        joiner.add(body);
        return joiner.toString();
    }

    private void failOnInvalidCommand() {
        if (commandType == null || commandType == MindCommandType.UNKNOWN) {
            throw new IllegalStateException("commandType cannot be " + commandType);
        }
    }

    private List<String> buildHeaderLines() {
        List<String> headerLines = new ArrayList<>();

        headerLines.add("Type: request");
        headerLines.add(String.format("RpcId: %d", client.nextRequestId()));
        headerLines.add(String.format("SchemaVersion: %d", MindRPC.SCHEMA_VER));
        headerLines.add("Content-Type: application/json");
        headerLines.add(String.format("RequestType: %s", commandType));
        headerLines.add("ResponseCount: single");
        headerLines.add("BodyId: ");
        headerLines.add(String.format("ApplicationName: %s", Archivo.APPLICATION_RDN));
        headerLines.add(String.format("ApplicationVersion: %s", Archivo.APPLICATION_VERSION));
        headerLines.add(String.format("ApplicationSessionId: 0x%x", client.getSessionId()));

        // Header always ends with a blank line
        headerLines.add("");

        return headerLines;
    }

    private void failOnInvalidResponse() throws IOException {
        if (response == null) {
            throw new IOException("No response received");
        }

        if (response.get("type").equals("error")) {
            logger.debug("Invalid response: {}", response.toString());
            throw new IOException(response.get("text").toString());
        }
    }

    @Override
    public String toString() {
        return String.format("MindCommand[type=%s, bodyData=%s]", commandType, bodyData);
    }
}
