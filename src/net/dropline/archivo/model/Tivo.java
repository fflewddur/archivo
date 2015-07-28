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

import net.dropline.archivo.net.MindRPC;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Base64;

public class Tivo {
    private final String name;
    private final String tsn;
    private final InetAddress[] addresses;
    private final int port;
    private final String mak;
    private MindRPC client;

    private static final String JSON_NAME = "name";
    private static final String JSON_TSN = "tsn";
    private static final String JSON_ADDRESSES = "addresses";
    private static final String JSON_PORT = "port";

    private Tivo(Builder builder) {
        name = builder.name;
        tsn = builder.tsn;
        addresses = builder.addresses;
        port = builder.port;
        mak = builder.mak;
    }

    public String getName() {
        return name;
    }

    public MindRPC getClient() {
        initRPCClientIfNeeded();
        return client;
    }

    private void initRPCClientIfNeeded() {
        if (client == null) {
            client = new MindRPC(addresses[0], port, mak);
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

    /**
     * Convert this Tivo to a JSON object.
     *
     * @return A new JSONObject representing this Tivo
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put(JSON_NAME, name);
        json.put(JSON_TSN, tsn);
        json.put(JSON_PORT, port);
        Base64.Encoder encoder = Base64.getEncoder();
        String[] encodedAddresses = new String[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            encodedAddresses[i] = encoder.encodeToString(addresses[i].getAddress());
        }
        json.put(JSON_ADDRESSES, new JSONArray(encodedAddresses));
        return json;
    }

    /**
     * Create a new Tivo object from a JSON String.
     *
     * @param json String containing the Tivo object in JSON
     * @param mak  Media access key to use for the resulting Tivo
     * @return A new Tivo object
     * @throws IllegalArgumentException
     */
    public static Tivo fromJSON(final String json, final String mak) throws IllegalArgumentException {
        JSONObject jo = new JSONObject(json);
        String name = jo.getString(JSON_NAME);
        String tsn = jo.getString(JSON_TSN);
        int port = jo.getInt(JSON_PORT);
        JSONArray jsonAddresses = jo.getJSONArray(JSON_ADDRESSES);
        InetAddress[] addresses = new InetAddress[jsonAddresses.length()];
        Base64.Decoder decoder = Base64.getDecoder();
        for (int i = 0; i < jsonAddresses.length(); i++) {
            try {
                addresses[i] = InetAddress.getByAddress(decoder.decode(jsonAddresses.getString(i)));
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("TiVo address in invalid: " + e.getLocalizedMessage());
            }
        }

        return new Builder().name(name).tsn(tsn).port(port).addresses(addresses).mak(mak).build();
    }

    public static class Builder {
        private String name;
        private InetAddress[] addresses;
        private String tsn;
        private int port;
        private String mak;

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

        public Builder mak(String val) {
            mak = val;
            return this;
        }

        public Tivo build() {
            failOnInvalidState();
            return new Tivo(this);
        }

        private void failOnInvalidState() {
            if (name == null) {
                throw new IllegalStateException("Field 'name' cannot be null");
            }
            if (mak == null) {
                throw new IllegalStateException("Field 'mak' cannot be null");
            }
            if (addresses == null) {
                throw new IllegalStateException("Field 'addresses' cannot be null");
            }
            if (tsn == null) {
                throw new IllegalStateException("Field 'tsn' cannot be null");
            }
            if (port == 0) {
                throw new IllegalStateException("Field 'port' cannot be 0");
            }
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
