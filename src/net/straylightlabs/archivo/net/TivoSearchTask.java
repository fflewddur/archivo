/*
 * Copyright 2015-2016 Todd Kulesza <todd@dropline.net>.
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

package net.straylightlabs.archivo.net;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import net.straylightlabs.archivo.model.Tivo;
import net.straylightlabs.hola.dns.Domain;
import net.straylightlabs.hola.sd.Instance;
import net.straylightlabs.hola.sd.Query;
import net.straylightlabs.hola.sd.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

/**
 * Use mDNS to identify TiVo devices on the local network.
 * Add identified devices to a collection of TiVos.
 */
public class TivoSearchTask extends Task<Void> {
    private final ObservableList<Tivo> tivos;
    private final String mak;
    private final int timeout;
    private boolean searchFailed;

    private static final String SERVICE_TYPE = "_tivo-mindrpc._tcp";
    private static final String IDENTIFYING_PROPERTY = "platform";
    private static final String PROPERTY_VALUE_STARTS_WITH = "tcd/";
    private static final String TSN_PROPERTY = "TSN";

    private final static Logger logger = LoggerFactory.getLogger(TivoSearchTask.class);

    public final static int SEARCH_TIMEOUT_SHORT = 750;
    public final static int SEARCH_TIMEOUT_LONG = 5000;

    public TivoSearchTask(ObservableList<Tivo> tivos, String mak, int timeout) {
        this.tivos = tivos;
        this.mak = mak;
        this.timeout = timeout;
    }

    @Override
    protected Void call() throws IOException {
        startSearch();
        return null;
    }

    public boolean searchFailed() {
        return searchFailed;
    }

    private void startSearch() {
        logger.info("Starting search for TiVo devices...");
        Service tivoMindService = Service.fromName(SERVICE_TYPE);
        Query query = Query.createWithTimeout(tivoMindService, Domain.LOCAL, timeout);
        try {
            List<Instance> instances = query.runOnce();
            logger.info("Found instances: {}", instances);
            addTivosFromInstances(instances);
            searchFailed = false;
        } catch (IOException e) {
            logger.error("Error searching for TiVo devices: ", e);
            searchFailed = true;
        }
    }

    private void addTivosFromInstances(List<Instance> instances) {
        instances.stream().filter(this::instanceIsSupportedTivo).forEach(instance -> {
            Tivo tivo = buildTivoFromInstance(instance);
            logger.info("New device: {}", tivo);
            // Add this device to our list, but use the JavaFX thread to do it.
            Platform.runLater(() -> {
                if (!tivos.contains(tivo)) {
                    tivos.add(tivo);
                } else {
                    logger.debug("Updating addresses for {}", tivo);
                    int index = tivos.indexOf(tivo);
                    Tivo existingTivo = tivos.get(index);
                    existingTivo.updateAddresses(tivo.getAddresses());
                }
            });
        });
    }

    /**
     * Examine the instances "TSN" attribute to determine if this is a supported TiVo device.
     */
    private boolean instanceIsSupportedTivo(Instance instance) {
        boolean isSupportedTivo = true;

        String tsn = getTSN(instance);
        if (tsn != null) {
            if (tsn.startsWith("A")) {
                // Streaming device
                isSupportedTivo = false;
            } else if (tsn.startsWith("0")) {
                // Series 1
                isSupportedTivo = false;
            } else if (tsn.startsWith("1") || tsn.startsWith("2") || tsn.startsWith("3") || tsn.startsWith("5")) {
                // Series 2
                isSupportedTivo = false;
            } else if (tsn.startsWith("6")) {
                // Series 3
                isSupportedTivo = false;
            }
            logger.info("isSupported = {} for instance {}", isSupportedTivo, instance);
        } else {
            logger.info("Instance {} has no TSN", instance);
            isSupportedTivo = false;
        }

        return isSupportedTivo;
    }

    private String getTSN(Instance instance) {
        String tsn = null;
        if (instance.hasAttribute("TSN")) {
            tsn = instance.lookupAttribute("TSN");
        } else if (instance.hasAttribute("tsn")) {
            tsn = instance.lookupAttribute("tsn");
        }
        return tsn;
    }

    private Tivo buildTivoFromInstance(Instance instance) {
        if (!instance.hasAttribute(IDENTIFYING_PROPERTY) || !instance.lookupAttribute(IDENTIFYING_PROPERTY).startsWith(PROPERTY_VALUE_STARTS_WITH)) {
            // Not a supported device
            logger.error("This does not look like a supported TiVo device.");
            throw new IllegalArgumentException("This does not look like a supported TiVo device.");
        }
        String tsn = instance.lookupAttribute(TSN_PROPERTY);
        List<InetAddress> addresses = instance.getAddresses();
        String name = instance.getName();
        int port = instance.getPort();
        return new Tivo.Builder().name(name).addresses(addresses).tsn(tsn).mak(mak).port(port).build();
    }
}
