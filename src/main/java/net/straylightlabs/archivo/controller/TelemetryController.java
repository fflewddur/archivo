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

package net.straylightlabs.archivo.controller;

import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import net.straylightlabs.archivo.Archivo;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Manage collecting and sending telemetry data.
 */
public class TelemetryController {
    private String userId;
    private boolean isEnabled;
    private final MixpanelAPI mixpanel;
    private final MessageBuilder messageBuilder;
    private final Deque<JSONObject> eventQueue;

    private final static Logger logger = LoggerFactory.getLogger(TelemetryController.class);
    private final static String MIXPANEL_TOKEN = "6f228676b079a226dc06923bd39f8a13";

    public TelemetryController() {
        mixpanel = new MixpanelAPI();
        messageBuilder = new MessageBuilder(MIXPANEL_TOKEN);
        eventQueue = new ArrayDeque<>();
    }

    public synchronized void setUserId(String userId) {
        if (this.userId != null) {
            logger.error("UserID is changing from {} to {}, this should never happen!", this.userId, userId);
        }
        this.userId = userId;
    }

    public synchronized void enable() {
        isEnabled = true;
    }

    public synchronized void disable() {
        isEnabled = false;
    }

    public synchronized void addEvent(String eventName, String... arguments) {
        JSONObject props = new JSONObject();
        for (int i = 1; i < arguments.length; i += 2) {
            props.put(arguments[i - 1], arguments[i]);
        }
        JSONObject event = messageBuilder.event(userId, eventName, props);
        eventQueue.addLast(event);
    }

    public synchronized void sendAll() {
        ClientDelivery delivery = new ClientDelivery();
        while (!eventQueue.isEmpty()) {
            delivery.addMessage(eventQueue.removeFirst());
        }

        if (isEnabled) {
            Thread sendThread = new Thread(() -> {
                try {
                    logger.debug("Sending telemetry...");
                    mixpanel.deliver(delivery);
                    logger.debug("Completed sending telemetry");
                } catch (IOException e) {
                    logger.error("Could not send telemetry: {}", e.getLocalizedMessage());
                }
            });
            sendThread.start();
        }
    }

    public synchronized void sendStartupEvent() {
        addEvent(
                "Startup",
                "OS Name", System.getProperty("os.name"), "OS Version", System.getProperty("os.version"),
                "Version", Archivo.APPLICATION_VERSION
        );
        JSONObject userDetails = new JSONObject();
        userDetails.put("OS Name", System.getProperty("os.name"));
        userDetails.put("OS Version", System.getProperty("os.version"));
        userDetails.put("Archivo Version", Archivo.APPLICATION_VERSION);
        eventQueue.addLast(messageBuilder.set(userId, userDetails));
        Map<String, Long> props = new HashMap<>();
        props.put("Invocations", 1L);
        eventQueue.addLast(messageBuilder.increment(userId, props));
        sendAll();
    }

    public synchronized void sendFoundTivosEvent(int numFound, int retriesNeeded) {
        JSONObject eventProps = new JSONObject();
        eventProps.put("Number Found", numFound);
        eventProps.put("Retries Needed", retriesNeeded);
        JSONObject event = messageBuilder.event(userId, "Found TiVos", eventProps);
        eventQueue.addLast(event);
        JSONObject userDetails = new JSONObject();
        userDetails.put("Found TiVos", numFound);
        userDetails.put("Search Retries", retriesNeeded);
        userDetails.put("No TiVos Found", numFound < 1);
        try {
            userDetails.put("Localhost", InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            logger.error("Error fetching localhost address: {}", e.getLocalizedMessage());
        }
        eventQueue.addLast(messageBuilder.set(userId, userDetails));
        sendAll();
    }

    public synchronized void sendNoTivosFoundEvent(int retries, boolean searchFailed) {
        JSONObject eventProps = new JSONObject();
        eventProps.put("Search Failed", searchFailed);
        eventProps.put("Retries", retries);
        JSONObject event = messageBuilder.event(userId, "No TiVos Found", eventProps);
        eventQueue.addLast(event);
        JSONObject userDetails = new JSONObject();
        userDetails.put("Found TiVos", 0);
        userDetails.put("Search Retries", retries);
        userDetails.put("No TiVos Found", true);
        int i = 0;
        for (String nic : getNetworkInterfaces()) {
            userDetails.put(String.format("Network Interface %d", i), nic);
            i++;
        }
        try {
            userDetails.put("Localhost", InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            logger.error("Error fetching localhost address: {}", e.getLocalizedMessage());
        }
        eventQueue.addLast(messageBuilder.set(userId, userDetails));
        sendAll();
    }

    public synchronized void sendArchivedEvent(int downloadDuration, int processingDuration, boolean cancelled) {
        JSONObject eventProps = new JSONObject();
        eventProps.put("Download Duration", downloadDuration);
        eventProps.put("Processing Duration", processingDuration);
        eventProps.put("Was Cancelled", cancelled);
        JSONObject event = messageBuilder.event(userId, "Recording Archived", eventProps);
        eventQueue.addLast(event);
        if (!cancelled) {
            Map<String, Long> props = new HashMap<>();
            props.put("Recordings Archived", 1L);
            eventQueue.addLast(messageBuilder.increment(userId, props));
        }
        sendAll();
    }

    public synchronized void sendArchiveFailedEvent(Throwable exception) {
        addEvent(
                "Archive Failed",
                "Error Message", exception.getMessage()
        );
        Map<String, Long> props = new HashMap<>();
        props.put("Failed Archives", 1L);
        eventQueue.addLast(messageBuilder.increment(userId, props));
        sendAll();
    }

    private static List<String> getNetworkInterfaces() {
        List<String> nics = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (nic.isUp())
                    nics.add(String.format("name='%s' isLoopback=%b isP2P=%b isVirtual=%b multicast=%b addresses=[%s]\n",
                            nic.getDisplayName(), nic.isLoopback(), nic.isPointToPoint(), nic.isVirtual(),
                            nic.supportsMulticast(), getAddressesAsString(nic)));
            }
        } catch (SocketException e) {
            logger.error("Error fetching network interface list: ", e);
        }
        return nics;
    }

    public static String getAddressesAsString(NetworkInterface nic) {
        StringJoiner sj = new StringJoiner(", ");
        for (InetAddress address : Collections.list(nic.getInetAddresses())) {
            sj.add(address.getHostAddress());

        }
        return sj.toString();
    }
}
