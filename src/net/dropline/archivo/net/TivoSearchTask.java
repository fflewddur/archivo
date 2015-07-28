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

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import net.dropline.archivo.model.Tivo;
import org.xbill.DNS.Message;
import org.xbill.mDNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

/**
 * Use mDNS to identify TiVo devices on the local network.
 * Returns a collection of discovered TiVos.
 */
public class TivoSearchTask extends Task<Void> {
    private final ObservableList<Tivo> tivos;
    private String mak;

    private static final String SERVICE_TYPE = "_tivo-mindrpc._tcp";
    private static final String IDENTIFYING_PROPERTY = "platform";
    private static final String PROPERTY_VALUE_STARTS_WITH = "tcd/";
    private static final String TSN_PROPERTY = "TSN";

    public TivoSearchTask(ObservableList<Tivo> tivos, String mak) {
        this.tivos = tivos;
        this.mak = mak;
    }

    @Override
    protected Void call() throws Exception {
//        System.out.println("Starting TiVo search...");
//        Lookup lookup = new Lookup(Constants.DEFAULT_BROWSE_DOMAIN_NAME, Constants.BROWSE_DOMAIN_NAME, Constants.LEGACY_BROWSE_DOMAIN_NAME, "local.");
////System.out.println("Domains: " + Constants.DEFAULT_BROWSE_DOMAIN_NAME + ", " + Constants.BROWSE_DOMAIN_NAME + ", " + Constants.LEGACY_BROWSE_DOMAIN_NAME);
//
//        Lookup.Domain[] domains = lookup.lookupDomains();
////        lookup.close();
//
//        Set<Name> domainNames = new HashSet<>();
//        for (Lookup.Domain domain : domains) {
//            System.out.println("domain: " + domain);
//            domainNames.add(domain.getName());
//        }
//        Name[] browseDomains = domainNames.toArray(new Name[domainNames.size()]);
//        lookup = new Lookup(Constants.ALL_MULTICAST_DNS_DOMAINS);
//        domains = lookup.lookupDomains();
//        System.out.println("DNS domains: " + Arrays.toString(domains));
//        System.out.println("browseDomains: " + Arrays.toString(browseDomains));
//        Name[] names = new Name[] {new Name("_tivo-mindrpc._tcp"), new Name("TiVo._tivo-mindrpc._tcp"), new Name("_http._tcp")};
//        lookup = new Lookup(names, Type.ANY, DClass.ANY);
//
//        System.out.println("response wait time: " + Querier.DEFAULT_RESPONSE_WAIT_TIME);
//        System.out.println("retry interval: " + Querier.DEFAULT_RETRY_INTERVAL);
//        System.out.println("search path: " + Arrays.toString(lookup.getSearchPath()));
//        System.out.println("Querier: " + lookup.getQuerier());
//        for (ServiceInstance instance : lookup.lookupServices()) {
//            System.out.println("Instance: " + instance);
//        }
//        for (Record instance : lookup.lookupRecords()) {
//            System.out.println("Record: " + instance);
//        }
//        System.out.println("Done");

//        lookup.close();
        startSearch();
        return null;
    }

    public void startSearch() throws IOException {
        MulticastDNSService mDNSService;
        Browse browse;
        Querier querier = MulticastDNSLookupBase.getDefaultQuerier();

        if (querier != null) {
            mDNSService = new MulticastDNSService();
            browse = new Browse(SERVICE_TYPE);
            mDNSService.startServiceDiscovery(browse, new DNSSDListener() {
                @Override
                public void serviceDiscovered(Object o, ServiceInstance serviceInstance) {
                    if (checkForCancellation(mDNSService)) {
                        return;
                    }

                    System.out.println("Discovered: " + serviceInstance);

                    try {
                        Tivo tivo = buildTivoFromServiceInstance(serviceInstance);
                        // Add this device to our list, but use the JavaFX thread to do it.
                        Platform.runLater(() -> {
                            if (!tivos.contains(tivo)) {
                                tivos.add(tivo);
                            }
                        });
                    } catch (IllegalArgumentException e) {
                        System.err.println("Discovered a device, but it doesn't look like a supported TiVo");
                    }
                }

                @Override
                public void serviceRemoved(Object o, ServiceInstance serviceInstance) {
                    if (checkForCancellation(mDNSService)) {
                        return;
                    }

                    System.out.println("Removed: " + serviceInstance);

                    try {
                        Tivo tivo = buildTivoFromServiceInstance(serviceInstance);
                        // Add this device to our list, but use the JavaFX thread to do it.
                        Platform.runLater(() -> tivos.remove(tivo));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Tried to remove a device, but it doesn't look like a supported TiVo");
                    }
                }

                @Override
                public void receiveMessage(Object o, Message message) {
                    // No need to check the return value; this method currently does nothing.
                    checkForCancellation(mDNSService);
                }

                @Override
                public void handleException(Object o, Exception e) {
                    System.err.println("Error while looking for TiVo devices: " + e.getLocalizedMessage());
                }
            });
        } else {
            System.err.println("Cannot start mDNS service because querier is not set up.");
        }
    }

    private boolean checkForCancellation(MulticastDNSService service) {
        if (isCancelled()) {
            System.out.println("Cancellation requested, closing mDNS service...");
            try {
                service.close();
            } catch (IOException e) {
                System.err.println("Error stopping mDNS service: " + e.getLocalizedMessage());
            }
            return true;
        }

        return false;
    }

    private Tivo buildTivoFromServiceInstance(ServiceInstance instance) throws IllegalArgumentException {
        @SuppressWarnings("unchecked") Map<String, String> textAttributes = instance.getTextAttributes();
        if (textAttributes == null) {
            // Not a supported device
            throw new IllegalArgumentException("This does not look like a supported TiVo device.");
        }
        String identifyingProperty = textAttributes.get(IDENTIFYING_PROPERTY);
        if (!identifyingProperty.startsWith(PROPERTY_VALUE_STARTS_WITH)) {
            // Not a supported device
            throw new IllegalArgumentException("This does not look like a supported TiVo device.");
        }
        String tsn = textAttributes.get(TSN_PROPERTY);
        InetAddress[] addresses = instance.getAddresses();
        String name = instance.getName().getInstance();
        int port = instance.getPort();
        return new Tivo.Builder().name(name).addresses(addresses).tsn(tsn).mak(mak).port(port).build();
    }
}
