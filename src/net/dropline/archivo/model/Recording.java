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

package net.dropline.archivo.model;

public class Recording {
    private final String seriesTitle;
    private final int seriesNumber;
    private final String episodeTitle;
    private final int episodeNumber;
    private final Channel channel;
    private final int minutesLong;

    public Recording(Builder builder) {
        seriesTitle = builder.seriesTitle;
        seriesNumber = builder.seriesNumber;
        episodeTitle = builder.episodeTitle;
        episodeNumber = builder.episodeNumber;
        channel = builder.channel;
        minutesLong = builder.minutesLong;
    }

    public String getSeriesTitle() {
        return seriesTitle;
    }

    public int getSeriesNumber() {
        return seriesNumber;
    }

    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public Channel getChannel() {
        return channel;
    }

    public int getDuration() {
        return minutesLong;
    }

    public static class Builder {
        private String seriesTitle;
        private int seriesNumber;
        private String episodeTitle;
        private int episodeNumber;
        private Channel channel;
        private int minutesLong;

        public Builder seriesTitle(String val) {
            seriesTitle = val;
            return this;
        }

        public Builder episodeTitle(String val) {
            episodeTitle = val;
            return this;
        }

        public Builder seriesNumber(int val) {
            seriesNumber = val;
            return this;
        }

        public Builder episodeNumber(int val) {
            episodeNumber = val;
            return this;
        }

        public Builder channel(String name, int number) {
            channel = new Channel(name, number);
            return this;
        }

        public Builder minutesLong(int val) {
            minutesLong = val;
            return this;
        }

        public Recording build() {
            return new Recording(this);
        }
    }
}
