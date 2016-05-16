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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

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
        while(!eventQueue.isEmpty()) {
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

    public synchronized void sendFoundTivosEvent(int numFound, int retriesNeeded, boolean searchFailed) {
        addEvent(
                "Found TiVos",
                "Number Found", Integer.toString(numFound),
                "Retries Needed", Integer.toString(retriesNeeded),
                "Search Failed", Boolean.toString(searchFailed)
        );
        sendAll();
    }

    public synchronized void sendArchivedEvent() {
        addEvent("Recording Archived");
        Map<String, Long> props = new HashMap<>();
        props.put("Recordings Archived", 1L);
        eventQueue.addLast(messageBuilder.increment(userId, props));
        sendAll();
    }
}
