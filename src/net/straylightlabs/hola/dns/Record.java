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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class Record {
    protected final String name;
    protected final Type type;

    protected final Class recordClass;

    public static Record fromBuffer(ByteBuffer buffer) {
        String name = readNameFromBuffer(buffer);
        Type type = Type.fromShort(buffer.getShort() & 0xFFFF);
        int rrClassByte = buffer.getShort() & 0x7FFF;
        Class recordClass = Class.fromShort(rrClassByte);
        int ttl = buffer.getInt();
        int rdLength = buffer.getShort() & 0xFFFF;
        if (rdLength > 0) {
            byte[] rdata = new byte[rdLength];
            buffer.get(rdata);
        }
        return new Record(name, type, recordClass);
    }

    public Record(Type type, Class recordClass) {
        this.name = null;
        this.type = type;
        this.recordClass = recordClass;
    }

    private Record(String name, Type type, Class recordClass) {
        this.name = name;
        this.type = type;
        this.recordClass = recordClass;
    }

    private static String readNameFromBuffer(ByteBuffer buffer) {
        List<String> labels = new ArrayList<>();
        int labelLength;
        do {
            labelLength = buffer.get() & 0xff;
            byte[] labelBuffer = new byte[labelLength];
            buffer.get(labelBuffer);
            labels.add(new String(labelBuffer));
        } while (labelLength != 0);
        return labels.stream().collect(Collectors.joining("."));
    }

    @Override
    public String toString() {
        return "Record{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", recordClass=" + recordClass +
                '}';
    }

    enum Type {
        PTR(12),
        ANY(255);

        private final int value;

        public static Type fromShort(int val) {
            for (Type type : values()) {
                if (type.value == val) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Can't convert " + val + " to a Type");
        }

        Type(int value) {
            this.value = value;
        }

        public short asShort() {
            return (short) (value & 0xffff);
        }
    }

    enum Class {
        IN(1);

        private final int value;

        public static Class fromShort(int val) {
            for (Class c : values()) {
                if (c.value == val) {
                    return c;
                }
            }
            throw new IllegalArgumentException("Can't convert " + val + " to a Class");
        }

        Class(int value) {
            this.value = value;
        }

        public short asShort() {
            return (short) (value & 0xffff);
        }
    }
}
