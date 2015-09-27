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

package net.straylightlabs.hola.dns;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

abstract class Record {
    protected final String name;
    protected final long ttl;

    protected final Class recordClass;

    public final static int USHORT_MASK = 0xFFFF;
    public final static long UINT_MASK = 0xFFFFFFFFL;
    public final static String NAME_CHARSET = "UTF-8";

    public static Record fromBuffer(ByteBuffer buffer) {
        String name = readNameFromBuffer(buffer);
        System.out.println("name: " + name);
        System.out.println("position: " + buffer.position());
        Type type = Type.fromInt(buffer.getShort() & USHORT_MASK);
//        int rrClassByte = buffer.getShort() & 0x7FFF;
        int tmp = buffer.getShort() & 0xFFFF;
        boolean flushCache = (tmp & 0x8000) == 0x8000;
        System.out.format("rrclass: 0x%08x type: %s flushCache: %s%n", tmp, type, flushCache);
        int rrClassByte = tmp & 0x7FFF;
        Class recordClass = Class.fromInt(rrClassByte);
        long ttl = buffer.getInt() & UINT_MASK;
        int rdLength = buffer.getShort() & USHORT_MASK;
        System.out.println("rdLength = " + rdLength);
//        byte[] rdata;
//        if (rdLength > 0) {
//            rdata = new byte[rdLength];
//            buffer.get(rdata);
//        } else {
//            rdata = new byte[0];
//        }

        switch (type) {
            case PTR:
                return new PtrRecord(buffer, name, recordClass, ttl);
            default:
                throw new IllegalArgumentException("Buffer represents an unsupported record type");
        }
    }

    protected Record(String name, Class recordClass, long ttl) {
        this.name = name;
        this.recordClass = recordClass;
        this.ttl = ttl;
    }

    public static String readNameFromBuffer(ByteBuffer buffer) {
        List<String> labels = new ArrayList<>();
        int labelLength;
        int continueFrom = -1;
        do {
            buffer.mark();
            labelLength = buffer.get() & 0xFF;
            if (isPointer(labelLength)) {
                buffer.reset();
                int offset = buffer.getShort() & 0x3FFF;
                if (continueFrom < 0) {
                    continueFrom = buffer.position();
                }
                buffer.position(offset);
            } else {
                String label = readLabel(buffer, labelLength);
                labels.add(label);
            }
        } while (labelLength != 0);

        if (continueFrom >= 0) {
            buffer.position(continueFrom);
        }

        return labels.stream().collect(Collectors.joining("."));
    }

    private static boolean isPointer(int octet) {
        return (octet & 0xC0) == 0xC0;
    }

    private static String readLabel(ByteBuffer buffer, int length) {
        String label = "";
        if (length > 0) {
            byte[] labelBuffer = new byte[length];
            buffer.get(labelBuffer);
            try {
                label = new String(labelBuffer, NAME_CHARSET);
            } catch (UnsupportedEncodingException e) {
                System.err.println("UnsupportedEncoding: " + e);
            }
        }
        return label;
    }

    @Override
    public String toString() {
        return "Record{" +
                "name='" + name + '\'' +
                ", recordClass=" + recordClass +
                ", ttl=" + ttl +
                '}';
    }

    enum Type {
        A(1),
        NS(2),
        CNAME(5),
        SOA(6),
        NULL(10),
        WKS(11),
        PTR(12),
        HINFO(13),
        MINFO(14),
        MX(15),
        TXT(16),
        AAAA(28),
        SRV(33);

        private final int value;

        public static Type fromInt(int val) {
            for (Type type : values()) {
                if (type.value == val) {
                    return type;
                }
            }
            throw new IllegalArgumentException(String.format("Can't convert 0x%04x to a Type", val));
        }

        Type(int value) {
            this.value = value;
        }

        public int asUnsignedShort() {
            return value & USHORT_MASK;
        }
    }

    enum Class {
        IN(1);

        private final int value;

        public static Class fromInt(int val) {
            for (Class c : values()) {
                if (c.value == val) {
                    return c;
                }
            }
            throw new IllegalArgumentException(String.format("Can't convert 0x%04x to a Class", val));
        }

        Class(int value) {
            this.value = value;
        }

        public int asUnsignedShort() {
            return value & USHORT_MASK;
        }
    }
}
