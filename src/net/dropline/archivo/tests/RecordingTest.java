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

package net.dropline.archivo.tests;

import net.dropline.archivo.model.Channel;
import net.dropline.archivo.model.Recording;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RecordingTest {
    @Test
    public void testBuilder() {
        Recording r = new Recording.Builder().seriesTitle("NOVA").seriesNumber(20).channel("OPB", 710).
                episodeTitle("Chasing Pluto").episodeNumber(14).minutesLong(59).build();

        assertEquals("Series title = 'NOVA'", "NOVA", r.getSeriesTitle());
        assertEquals("Series number = 20", 20, r.getSeriesNumber());
        assertEquals("Episode title = 'Chasing Pluto'", "Chasing Pluto", r.getEpisodeTitle());
        assertEquals("Episode number = 14", 14, r.getEpisodeNumber());
        assertEquals("Duration = 59", 59, r.getDuration());
        Assert.assertEquals("Channel = [OPB (710)]", new Channel("OPB", 710), r.getChannel());

        assertNotEquals("Series title != 'American Experience'", "American Experience", r.getSeriesTitle());
        assertNotEquals("Series number != 4", 4, r.getSeriesNumber());
        assertNotEquals("Episode title != 'Dorothea Lange'", "Dorothea Lange", r.getEpisodeTitle());
        assertNotEquals("Episode number != 12", 12, r.getEpisodeNumber());
        assertNotEquals("Duration != 45", 45, r.getDuration());
        assertNotEquals("Channel != [OPB (10)]", new Channel("OPB", 10), r.getChannel());
    }

}