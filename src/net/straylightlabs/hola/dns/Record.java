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

abstract class Record {
    protected final Type type;
    protected final Class recordClass;

    public Record(Type type, Class recordClass) {
        this.type = type;
        this.recordClass = recordClass;
    }

    enum Type {
        PTR(12),
        ANY(255);

        private final int value;

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

        Class(int value) {
            this.value = value;
        }

        public short asShort() {
            return (short) (value & 0xffff);
        }
    }
}
