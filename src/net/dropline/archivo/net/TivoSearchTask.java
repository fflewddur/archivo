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

import javafx.concurrent.Task;
import net.dropline.archivo.model.Tivo;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * Use mDNS to identify TiVo devices on the local network.
 * Returns a collection of discovered TiVos.
 */
public class TivoSearchTask extends Task<Set<Tivo>> {
    private static final JmDNS jmdns;

    private static final String SERVICE_PREFIX = "_tivo-mindrpc._tcp.local.";
    private static final String IDENTIFYING_PROPERTY = "platform";
    private static final String PROPERTY_VALUE_STARTS_WITH = "tcd/";
    private static final String TSN_PROPERTY = "TSN";

    static {
        JmDNS temp = null;
        try {
            temp = JmDNS.create(InetAddress.getLocalHost());
        } catch (IOException e) {
            System.err.println("Error starting TiVo search service: " + e.getLocalizedMessage());
        } finally {
            jmdns = temp;
        }
    }

    @Override
    protected synchronized Set<Tivo> call() throws Exception {
        Set<Tivo> tivos = new HashSet<>();
        ServiceInfo[] infoList = jmdns.list(SERVICE_PREFIX);
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
}
