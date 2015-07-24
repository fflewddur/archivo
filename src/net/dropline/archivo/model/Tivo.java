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

import net.dropline.archivo.MainApp;
import net.dropline.archivo.net.MindCommand;
import net.dropline.archivo.net.MindCommandRecordingFolderItemSearch;
import net.dropline.archivo.net.MindRPC;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Tivo {
    private final String name;
    private final String tsn;
    private final InetAddress[] addresses;
    private final int port;
    private MindRPC client;

    private Tivo(Builder builder) {
        name = builder.name;
        tsn = builder.tsn;
        addresses = builder.addresses;
        port = builder.port;
    }

    public String getName() {
        return name;
    }

    public List<Recording> getRecordings() {
        initRPCClient();
        MindCommand command = new MindCommandRecordingFolderItemSearch();
        command.executeOn(client, null);
        return Collections.emptyList();
    }

    private void initRPCClient() {
        if (client == null) {
            client = new MindRPC(addresses[0], port, MainApp.testDeviceMAK);
        }
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + tsn.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Tivo)) {
            return false;
        }

        Tivo o = (Tivo) obj;
        return (name.equals(o.name) && tsn.equals(o.tsn));
    }

    @Override
    public String toString() {
        return String.format("Tivo[name=%s, tsn=%s, addresses=%s, port=%d]", name, tsn, Arrays.toString(addresses), port);
    }

    public static class Builder {
        private String name;
        private InetAddress[] addresses;
        private String tsn;
        private int port;

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder addresses(InetAddress[] val) {
            addresses = val;
            return this;
        }

        public Builder tsn(String val) {
            tsn = val;
            return this;
        }

        public Builder port(int val) {
            port = val;
            return this;
        }

        public Tivo build() {
            return new Tivo(this);
        }
    }

    public static class StringConverter extends javafx.util.StringConverter<Tivo> {

        @Override
        public String toString(Tivo object) {
            return object.getName();
        }

        @Override
        public Tivo fromString(String string) {
            return null;
        }
    }
}
