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

import net.dropline.archivo.Main;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public abstract class MindCommand {
    private MindRPC manager;
    protected MindCommandType commandType;
    protected JSONObject bodyData;

    protected MindCommand() {
        this.commandType = MindCommandType.UNKNOWN;
        this.bodyData = new JSONObject();
    }

    public void executeOn(MindRPC manager, MindCommandCompletedHandler completedHandler) {
        this.manager = manager;
        String request = buildRequest();
        manager.send(request, completedHandler);
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
        headerLines.add(String.format("RpcId: %d", manager.nextRequestId()));
        headerLines.add(String.format("SchemaVersion: %d", MindRPC.SCHEMA_VER));
        headerLines.add("Content-Type: application/json");
        headerLines.add(String.format("RequestType: %s", commandType));
        headerLines.add("ResponseCount: single");
        headerLines.add("BodyId: ");
        headerLines.add(String.format("ApplicationName: %s", Main.ApplicationRDN));
        headerLines.add(String.format("ApplicationVersion: %s", Main.ApplicationVersion));
        headerLines.add(String.format("ApplicationSessionId: 0x%x", manager.getSessionId()));

        // Header always ends with a blank line
        headerLines.add("");

        return headerLines;
    }
}
