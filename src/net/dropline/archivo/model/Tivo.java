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

package net.dropline.archivo.model;

import net.dropline.archivo.model.Recording;
import net.dropline.archivo.net.MindCommand;
import net.dropline.archivo.net.MindCommandRecordingFolderItemSearch;
import net.dropline.archivo.net.MindRPC;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

public class Tivo {
    private final String name;
    private final InetAddress address;
    private final String mak;
    private final MindRPC client;

    public Tivo(String name, InetAddress address, String mak) {
        this.name = name;
        this.address = address;
        this.mak = mak;
        client = new MindRPC(this.address, this.mak);
    }

    public String getName() {
        return name;
    }

    public List<Recording> getRecordings() throws IOException {
//        client.openConnection(new URL("https://10.0.0.110:"))

        MindCommand command = new MindCommandRecordingFolderItemSearch();
        command.executeOn(client, null);

//        SSLSocket sock = client.connect();
//        Socket sock = client.connectInsecure();
//        System.out.println("writing to socket...");
//        sock.getInputStream();
//        PrintWriter writer = new PrintWriter(sock.getOutputStream());
//        writer.print("MRPC/2 124 18\r\n" +
//                "Type: request\r\n" +
//                "RpcId: 100/\r\n" +
//                "SchemaVersion: 7\n" +
//                "Content-Type: application/json\n" +
//                "RequestType: infoGet\n" +
//                "ResponseCount: single\n\n" +
//                "{\"type\":\"infoGet\"}\n");
//        writer.flush();
//        System.out.println("post flush");
//        BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
//        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
//            System.out.println("looping");
//            System.out.println(line);
//        }
//        writer.close();
//        reader.close();
//        sock.close();
        return Collections.emptyList();
    }
}
