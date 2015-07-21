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

import javafx.beans.property.*;

import java.time.LocalDateTime;

public class Recording {
    private final StringProperty seriesTitle;
    private final StringProperty episodeTitle;
    private final ObjectProperty<LocalDateTime> dateRecorded;
    private final IntegerProperty minutesLong;

    private final int seriesNumber;
    private final int episodeNumber;
    private final Channel channel;

    public Recording(Builder builder) {
        seriesTitle = new SimpleStringProperty(builder.seriesTitle);
        episodeTitle = new SimpleStringProperty(builder.episodeTitle);
        dateRecorded = new SimpleObjectProperty<>(builder.dateRecorded);
        minutesLong = new SimpleIntegerProperty(builder.minutesLong);

        // FIXME these need to become Properties
        seriesNumber = builder.seriesNumber;
        episodeNumber = builder.episodeNumber;
        channel = builder.channel;
    }

    public String getSeriesTitle() {
        return seriesTitle.get();
    }

    public StringProperty seriesTitleProperty() {
        return seriesTitle;
    }

    public String getEpisodeTitle() {
        return episodeTitle.get();
    }

    public StringProperty episodeTitleProperty() {
        return episodeTitle;
    }

    public LocalDateTime getDateRecorded() {
        return dateRecorded.get();
    }

    public ObjectProperty<LocalDateTime> dateRecordedProperty() {
        return dateRecorded;
    }

    public int getDuration() {
        return minutesLong.get();
    }

    public IntegerProperty durationProperty() {
        return minutesLong;
    }

    public int getSeriesNumber() {
        return seriesNumber;
    }
    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public Channel getChannel() {
        return channel;
    }



    public static class Builder {
        private String seriesTitle;
        private int seriesNumber;
        private String episodeTitle;
        private int episodeNumber;
        private Channel channel;
        private int minutesLong;
        private LocalDateTime dateRecorded;

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

        public Builder recordedOn(LocalDateTime val) {
            dateRecorded = val;
            return this;
        }

        public Recording build() {
            return new Recording(this);
        }
    }
}
