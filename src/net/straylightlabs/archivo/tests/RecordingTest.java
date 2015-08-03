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

package net.straylightlabs.archivo.tests;

import net.straylightlabs.archivo.model.Channel;
import net.straylightlabs.archivo.model.Recording;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RecordingTest {
    @Test
    public void testBuilder() {
        Recording r = new Recording.Builder().seriesTitle("NOVA").seriesNumber(20).channel("OPB", "710", null).
                episodeTitle("Chasing Pluto").episodeNumbers(new ArrayList<>(14)).secondsLong(59).build();

        assertEquals("Series title = 'NOVA'", "NOVA", r.getSeriesTitle());
        assertEquals("Episode title = 'Chasing Pluto'", "Chasing Pluto", r.getEpisodeTitle());
        Assert.assertEquals("Channel = [OPB (710)]", new Channel("OPB", "710", null), r.getChannel());

        assertNotEquals("Series title != 'American Experience'", "American Experience", r.getSeriesTitle());
        assertNotEquals("Episode title != 'Dorothea Lange'", "Dorothea Lange", r.getEpisodeTitle());
        assertNotEquals("Channel != [OPB (10)]", new Channel("OPB", "10", null), r.getChannel());
    }

}