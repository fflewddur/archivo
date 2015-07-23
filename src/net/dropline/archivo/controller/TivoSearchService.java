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

package net.dropline.archivo.controller;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.dropline.archivo.model.Tivo;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class TivoSearchService extends Service<Set<Tivo>> {
    private JmDNS jmdns;

    private static final String SERVICE_PREFIX = "_tivo-mindrpc._tcp.local.";
    private static final String IDENTIFYING_PROPERTY = "platform";
    private static final String PROPERTY_VALUE_STARTS_WITH = "tcd/";
    private static final String TSN_PROPERTY = "TSN";

    public TivoSearchService() {
        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost());
            jmdns.unregisterAllServices();
        } catch (IOException e) {
            System.err.println("Error searching for TiVo devices: " + e.getLocalizedMessage());
        }
    }

    @Override
    protected Task<Set<Tivo>> createTask() {
        return new Task<Set<Tivo>>() {
            @Override
            protected Set<Tivo> call() throws Exception {
                System.out.println("listing...");
                Set<Tivo> tivos = new HashSet<>();
                ServiceInfo[] infoList = jmdns.list(SERVICE_PREFIX);
                System.out.println("list complete!");
                for (ServiceInfo info : infoList) {
                    String identifyingProperty = info.getPropertyString(IDENTIFYING_PROPERTY);
                    if (!identifyingProperty.startsWith(PROPERTY_VALUE_STARTS_WITH)) {
                        // Not a supported device
                        continue;
                    }
                    String tsn = info.getPropertyString(TSN_PROPERTY);
                    InetAddress[] addresses = info.getInetAddresses();
                    String name = info.getName();
                    int port = info.getPort();
                    tivos.add(new Tivo.Builder().name(name).addresses(addresses).tsn(tsn).port(port).build());
                }
                return tivos;
            }
        };
    }
}
