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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authorize ourselves using the TiVo's MAK. This needs to happen before any other RPC commands can be sent over
 * the socket.
 */
class MindCommandAuth extends MindCommand {
    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(MindCommandAuth.class);

    public MindCommandAuth(String mak) {
        super();
        commandType = MindCommandType.AUTH;

        JSONObject credential = new JSONObject();
        credential.put("type", "makCredential");
        credential.put("key", mak);

        bodyData.put("credential", credential);
    }

    public boolean credentialsRejected() {
        return (response != null && response.get("status").equals("failure"));
    }
}
