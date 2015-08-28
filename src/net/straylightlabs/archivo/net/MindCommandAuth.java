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

package net.straylightlabs.archivo.net;

import org.json.JSONObject;

/**
 * Authorize ourselves using the TiVo's MAK. This needs to happen before any other RPC commands can be sent over
 * the socket.
 */
class MindCommandAuth extends MindCommand {
    public MindCommandAuth(String mak) {
        super();
        commandType = MindCommandType.AUTH;

        JSONObject credential = new JSONObject();
        credential.put("type", "makCredential");
        credential.put("key", mak);

        bodyData.put("credential", credential);
    }
}
