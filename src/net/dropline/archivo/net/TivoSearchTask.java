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
import org.xbill.DNS.Message;
import org.xbill.mDNS.*;

import java.net.InetAddress;
import java.util.Map;

/**
 * Use mDNS to identify TiVo devices on the local network.
 * Returns a collection of discovered TiVos.
 */
public class TivoSearchTask extends Task<Void> {
    private static final String SERVICE_TYPE = "_tivo-mindrpc._tcp";
    private static final String IDENTIFYING_PROPERTY = "platform";
    private static final String PROPERTY_VALUE_STARTS_WITH = "tcd/";
    private static final String TSN_PROPERTY = "TSN";

    public TivoSearchTask() {
    }

    @Override
    protected Void call() throws Exception {
        MulticastDNSService mDNSService;
        Browse browse;
        Querier querier = MulticastDNSLookupBase.getDefaultQuerier();
        if (querier != null) {
            mDNSService = new MulticastDNSService();
            browse = new Browse(SERVICE_TYPE);

            mDNSService.startServiceDiscovery(browse, new DNSSDListener() {
                @Override
                public void serviceDiscovered(Object o, ServiceInstance serviceInstance) {
                    System.out.println("Discovered: " + serviceInstance);
                    @SuppressWarnings("unchecked") Map<String, String> textAttributes = serviceInstance.getTextAttributes();
                    if (textAttributes == null) {
                        // Not a supported device
                        return;
                    }
                    String identifyingProperty = textAttributes.get(IDENTIFYING_PROPERTY);
                    if (!identifyingProperty.startsWith(PROPERTY_VALUE_STARTS_WITH)) {
                        // Not a supported device
                        return;
                    }
                    String tsn = textAttributes.get(TSN_PROPERTY);
                    InetAddress[] addresses = serviceInstance.getAddresses();
                    String name = serviceInstance.getName().getInstance();
                    int port = serviceInstance.getPort();
                    Tivo tivo = new Tivo.Builder().name(name).addresses(addresses).tsn(tsn).port(port).build();
                    System.out.println("Tivo: " + tivo);
                    // TODO now we need to get this device into our model
//                    tivos.add(new Tivo.Builder().name(name).addresses(addresses).tsn(tsn).port(port).build());
                }

                @Override
                public void serviceRemoved(Object o, ServiceInstance serviceInstance) {
                    System.out.println("Removed: " + serviceInstance);
                }

                @Override
                public void receiveMessage(Object o, Message message) {
                }

                @Override
                public void handleException(Object o, Exception e) {
                    System.err.println("Error while looking for TiVo devices: " + e.getLocalizedMessage());
                }
            });
        } else {
            System.err.println("Cannot start mDNS-discovery because querier is not set up.");
        }

        return null;
    }
}
