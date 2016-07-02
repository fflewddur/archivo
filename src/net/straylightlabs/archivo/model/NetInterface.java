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

package net.straylightlabs.archivo.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Convenience class for holding a network interface and it's IPv4 addresses.
 * TiVo's only support IPv4, so we can disregard IPv6 addresses entirely.
 */
public class NetInterface {
    private final NetworkInterface networkInterface;
    private final List<InetAddress> addresses;
    private final String machineRepresentation;
    private final boolean isDefault;

    public static final NetInterface DEFAULT = new NetInterface();
    public static final String DEFAULT_DESCRIPTION = "my primary network";
    public static final String DEFAULT_MACHINE_REPRESENTATION = "auto";

    private final static Logger logger = LoggerFactory.getLogger(NetInterface.class);

    /**
     * Default constructor to model the OS's default network interface
     */
    public NetInterface() {
        isDefault = true;
        machineRepresentation = DEFAULT_MACHINE_REPRESENTATION;
        NetworkInterface networkInterface = null;
        addresses = new ArrayList<>();
        try {
            InetAddress localhost = Inet4Address.getLocalHost();
            networkInterface = NetworkInterface.getByInetAddress(localhost);
            addresses.add(localhost);
        } catch (UnknownHostException e) {
            logger.error("Unable to get localhost address: {}", e.getLocalizedMessage());
        } catch (SocketException e) {
            logger.error("Unable to get localhost's network interface: {}", e.getLocalizedMessage());
        } finally {
            this.networkInterface = networkInterface;
        }
    }

    /**
     * Constructor to model the given NetworkInterface
     * @param networkInterface network interface to model
     */
    public NetInterface(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
        this.addresses = filterIPv4Addresses();
        isDefault = false;
        String machineRepresentation = null;
        try {
            byte[] macBytes = networkInterface.getHardwareAddress();
            if (macBytes != null) {
                machineRepresentation = new String(macBytes);
            }
        } catch (SocketException e) {
            logger.error("Unable to get hardware address for interface '{}': {}",
                    networkInterface.getDisplayName(), e.getLocalizedMessage());
        } finally {
            this.machineRepresentation = machineRepresentation;
        }
    }

    private List<InetAddress> filterIPv4Addresses() {
        List<InetAddress> addresses = new ArrayList<>();
        Collections.list(networkInterface.getInetAddresses()).stream()
                .filter(address -> address instanceof Inet4Address).forEach(addresses::add);
        return addresses;
    }

    public InetAddress getFirstAddress() {
        if (addresses.size() > 0) {
            return addresses.get(0);
        } else {
            throw new RuntimeException("Trying to connect to a network that you don't have an IPv4 address for.");
        }
    }

    public boolean isRealIPv4Multicast() {
        try {
            return networkInterface.isUp() && !networkInterface.isVirtual() && networkInterface.supportsMulticast()
                    && addresses.size() > 0 && !networkInterface.isLoopback();
        } catch (SocketException e) {
            return false;
        }
    }

    /**
     * Return the hash code for this interface's hardware address
     */
    public int getMachineHash() {
        if (isDefault) {
            return DEFAULT_MACHINE_REPRESENTATION.hashCode();
        } else {
            return machineRepresentation.hashCode();
        }
    }

    @Override
    public String toString() {
        if (isDefault) {
            return DEFAULT_DESCRIPTION;
        } else if (addresses.size() > 0) {
            return String.format("%s (%s)", networkInterface.getDisplayName(), addresses.get(0).getHostAddress());
        } else {
            return "Unsupported network";
        }
    }

    @Override
    public int hashCode() {
        return getMachineHash();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NetInterface)) {
            return false;
        } else if (obj == this) {
            return true;
        }

        NetInterface other = (NetInterface) obj;
        return isDefault && other.isDefault || getMachineHash() == other.getMachineHash();
    }
}
