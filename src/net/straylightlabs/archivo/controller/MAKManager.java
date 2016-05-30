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

import java.util.*;

/**
 * Maintain a sorted list of MAKs this user has entered, sorted by most-recently used
 */
public class MAKManager {
    private final List<String> maks;
    private final Set<String> failedMaks;

    public static final String SEPARATOR = ";";

    public MAKManager() {
        maks = new ArrayList<>();
        failedMaks = new HashSet<>();
    }

    /**
     * Returns the most-recently used MAK
     *
     * @return the most-recently used MAK
     */
    public String currentMAK() {
        if (maks.size() > 0) {
            return maks.get(0);
        } else {
            return null;
        }
    }

    /**
     * Mark the current MAK as having failed to authenticate and try the next MAK that hasn't failed.
     *
     * @return The next MAK, or null if all MAKs have failed
     */
    public String tryNextMAK() {
        String currentMak = currentMAK();
        failedMaks.add(currentMak);
        String nextMak = null;
        for (String mak : maks) {
            if (!failedMaks.contains(mak)) {
                nextMak = mak;
                break;
            }
        }
        if (nextMak != null) {
            maks.remove(nextMak);
            maks.add(0, nextMak);
        }
        return nextMak;
    }

    public void addMAK(String mak) {
        if (mak == null || mak.isEmpty()) {
            return;
        }
        if (maks.contains(mak)) {
            maks.remove(mak);
        }
        maks.add(0, mak);
    }

    /**
     * Load a separated string of recently-used MAKs
     *
     * @param makString String of MAKs to load
     */
    public void load(String makString) {
        maks.clear();
        Arrays.asList(makString.split(MAKManager.SEPARATOR)).stream().forEach(this::addMAK);
    }

    /**
     * Create a separated string of recently-used MAKs
     *
     * @return String of MAKS to save
     */
    public String getAsString() {
        StringJoiner sj = new StringJoiner(SEPARATOR);
        maks.forEach(sj::add);
        return sj.toString();
    }
}
