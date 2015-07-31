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

package net.straylightlabs.archivo.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Models a single recording from a TiVo device.
 */
public class Recording {
    // Items displayed in the RecordingListView need to be observable properties
    private final StringProperty seriesTitle;
    private final StringProperty episodeTitle;
    private final ObjectProperty<LocalDateTime> dateRecorded;

    private final Duration duration;
    private final int seriesNumber;
    private final List<Integer> episodeNumbers;
    private final Channel channel;
    private final String description;
    private final URL imageURL;
    private final LocalDate originalAirDate;
    private final RecordingState state;
    private final RecordingReason reason;
    private final boolean isCopyable;

    // Denotes Recordings used as the header line for the series in RecordingListView
    private final boolean isSeriesHeading;
    // Combine season and episode number(s) into a more useful string
    private final String seasonAndEpisode;

    public final static int DESIRED_IMAGE_WIDTH = 200;
    public final static int DESIRED_IMAGE_HEIGHT = 150;

    private Recording(Builder builder) {
        seriesTitle = new SimpleStringProperty(builder.seriesTitle);
        episodeTitle = new SimpleStringProperty(builder.episodeTitle);
        dateRecorded = new SimpleObjectProperty<>(builder.dateRecorded);

        duration = Duration.ofSeconds(builder.secondsLong);
        seriesNumber = builder.seriesNumber;
        episodeNumbers = builder.episodeNumbers;
        channel = builder.channel;
        description = builder.description;
        imageURL = builder.imageURL;
        originalAirDate = builder.originalAirDate;
        state = builder.state;
        reason = builder.reason;
        isCopyable = builder.isCopyable;

        isSeriesHeading = builder.isSeriesHeading;
        seasonAndEpisode = buildSeasonAndEpisode(seriesNumber, episodeNumbers);
    }

    private String buildSeasonAndEpisode(int seriesNumber, List<Integer> episodeNumbers) {
        StringBuilder sb = new StringBuilder();
        int numEpisodes = episodeNumbers.size();
        if (seriesNumber > 0) {
            sb.append(String.format("Season %d", seriesNumber));
            if (numEpisodes > 0) {
                sb.append(" ");
            }
        }
        if (numEpisodes > 0) {
            if (numEpisodes > 1) {
                sb.append("Episodes ");
                for (int i = 0; i < numEpisodes - 2; i++) {
                    sb.append(String.format("%d, ", episodeNumbers.get(i)));
                }
                sb.append(String.format("%d & %d", episodeNumbers.get(numEpisodes - 2), episodeNumbers.get(numEpisodes - 1)));
            } else {
                sb.append(String.format("Episode %d", episodeNumbers.get(0)));
            }
        }
        return sb.toString();
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

    public Duration getDuration() {
        return duration;
    }

    public boolean hasSeasonAndEpisode() {
        return (seriesNumber > 0 || episodeNumbers.size() > 0);
    }

    public String getSeasonAndEpisode() {
        return seasonAndEpisode;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getDescription() {
        return description;
    }

    public URL getImageURL() {
        return imageURL;
    }

    public LocalDate getOriginalAirDate() {
        return originalAirDate;
    }

    public RecordingState getState() {
        return state;
    }

    public RecordingReason getReason() {
        return reason;
    }

    public boolean isCopyable() {
        return isCopyable;
    }

    public boolean isSeriesHeading() {
        return isSeriesHeading;
    }

    public static class Builder {
        private String seriesTitle;
        private int seriesNumber;
        private String episodeTitle;
        private List<Integer> episodeNumbers;
        private Channel channel;
        private int secondsLong;
        private LocalDateTime dateRecorded;
        private String description;
        private URL imageURL;
        private LocalDate originalAirDate;
        private RecordingState state;
        private RecordingReason reason;
        private boolean isCopyable;
        private boolean isSeriesHeading;

        public Builder() {
            // Set default values
            episodeNumbers = Collections.emptyList();
            description = "";
            state = RecordingState.UNKNOWN;
            reason = RecordingReason.UNKNOWN;
            isSeriesHeading = false;
        }

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

        public Builder episodeNumbers(List<Integer> val) {
            episodeNumbers = val;
            return this;
        }

        public Builder channel(String name, String number) {
            channel = new Channel(name, number);
            return this;
        }

        public Builder secondsLong(int val) {
            secondsLong = val;
            return this;
        }

        public Builder recordedOn(LocalDateTime val) {
            dateRecorded = val;
            return this;
        }

        public Builder description(String val) {
            description = val;
            return this;
        }

        public Builder image(URL val) {
            imageURL = val;
            return this;
        }

        public Builder originalAirDate(LocalDate val) {
            originalAirDate = val;
            return this;
        }

        public Builder state(RecordingState val) {
            state = val;
            return this;
        }

        public Builder reason(RecordingReason val) {
            reason = val;
            return this;
        }

        public Builder copyable(boolean val) {
            isCopyable = val;
            return this;
        }

        public Builder isSeriesHeading(boolean val) {
            isSeriesHeading = val;
            return this;
        }

        public Recording build() {
            return new Recording(this);
        }
    }
}
