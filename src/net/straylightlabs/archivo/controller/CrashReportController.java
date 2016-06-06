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

import net.straylightlabs.archivo.Archivo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Track whether we should upload a crash report. If so, build a task to upload it from a separate thread.
 */
public class CrashReportController {
    private boolean hasCrashReport;
    private final String userId;

    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(CrashReportController.class);

    public CrashReportController(String userId) {
        this.userId = userId;
        this.hasCrashReport = true;
    }

    public void crashOccurred() {
        hasCrashReport = true;
    }

    public boolean hasCrashReport() {
        return hasCrashReport;
    }

    public CrashReportTask buildTask() {
        return new CrashReportTask(userId, Archivo.LOG_PATH);
    }
}
