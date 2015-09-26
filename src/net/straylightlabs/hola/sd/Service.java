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

package net.straylightlabs.hola.sd;

import java.util.Arrays;
import java.util.List;

public class Service {
    private final String name;
    private final List<String> labels;

    public static Service fromName(String name) {
        return new Service(name);
    }

    private Service(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("A Service's name can't be null or empty");
        }

        this.name = name;
        labels = Arrays.asList(name.split("\\."));
    }

    public List<String> getLabels() {
        return labels;
    }

    @Override
    public String toString() {
        return "Service{" +
                "name='" + name + '\'' +
                ", labels=" + labels +
                '}';
    }
}
