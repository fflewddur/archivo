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

import javafx.beans.property.*;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Models a single recording from a TiVo device.
 */
public class Recording {
    // Items displayed in the RecordingListView need to be observable properties
    private final StringProperty title;
    private final ObjectProperty<LocalDateTime> dateRecorded;
    private final ObjectProperty<ArchiveStatus> status;
    // Denotes Recordings used as the header line for the series in RecordingListView
    private final BooleanProperty isSeriesHeading;
    private final BooleanProperty isArchivable;
    private final BooleanProperty isCancellable;
    private final BooleanProperty isPlayable;
    private final BooleanProperty isRemovable;

    private final String recordingId;
    private final String bodyId;
    private final String seriesTitle;
    private final String episodeTitle;
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
    private final LocalDateTime expectedDeletion;
    private final Type collectionType;

    // Denotes Recordings that are child nodes in the RecordingListView
    private boolean isChildRecording;
    private final int numEpisodes;
    // Combine season and episode number(s) into a more useful string
    private final String seasonAndEpisode;
    private Path destination;
    private FileType destinationType;

    public final static int DESIRED_IMAGE_WIDTH = 200;
    public final static int DESIRED_IMAGE_HEIGHT = 150;
    private final static char EM_DASH = '\u2014';
    private final static String UNTITLED_TEXT = "Untitled";

    private Recording(Builder builder) {
        recordingId = builder.recordingId;
        bodyId = builder.bodyId;
        seriesTitle = builder.seriesTitle;
        episodeTitle = builder.episodeTitle;
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
        expectedDeletion = builder.expectedDeletion;
        collectionType = builder.collectionType;

        isChildRecording = builder.isChildRecording;
        numEpisodes = builder.numEpisodes;
        seasonAndEpisode = buildSeasonAndEpisode(seriesNumber, episodeNumbers);

        isSeriesHeading = new SimpleBooleanProperty(builder.isSeriesHeading);
        title = new SimpleStringProperty(buildTitle());
        dateRecorded = new SimpleObjectProperty<>(builder.dateRecorded);
        status = new SimpleObjectProperty<>(ArchiveStatus.EMPTY);
        isRemovable = new SimpleBooleanProperty(!builder.isSeriesHeading);
        isArchivable = new SimpleBooleanProperty(isArchivable());
        isCancellable = new SimpleBooleanProperty(false);
        isPlayable = new SimpleBooleanProperty(false);
    }

    /**
     * Build a UI-friendly String describing the season and episode number.
     */
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

    /**
     * Build a UI-friendly title that consists of the series name + episode name for regular recordings,
     * and just the series name for header recordings.
     */
    private String buildTitle() {
        if (isSeriesHeading.get()) {
            return buildSeriesHeadingTitle();
        } else if (isChildRecording) {
            return buildChildRecordingTitle();
        } else {
            return buildSingleRecordingTitle();
        }
    }

    private String buildSeriesHeadingTitle() {
        if (seriesTitle != null) {
            return seriesTitle;
        } else {
            return UNTITLED_TEXT;
        }
    }

    private String buildChildRecordingTitle() {
        if (episodeTitle != null) {
            return episodeTitle;
        } else if (seriesTitle != null && hasSeasonAndEpisode()) {
            return String.format("%s %c %s", seriesTitle, EM_DASH, seasonAndEpisode);
        } else if (seriesTitle != null) {
            return seriesTitle;
        } else {
            return UNTITLED_TEXT;
        }
    }

    /**
     * Build a title for a recording that is not grouped with others from the same series.
     */
    private String buildSingleRecordingTitle() {
        if (seriesTitle != null && episodeTitle != null) {
            return String.format("%s %c %s", seriesTitle, EM_DASH, episodeTitle);
        } else if (seriesTitle != null) {
            return seriesTitle;
        } else if (episodeTitle != null) {
            return episodeTitle;
        } else {
            return UNTITLED_TEXT;
        }
    }

    private boolean isArchivable() {
        return isCopyable && !isSeriesHeading.get() && !isInProgress() && !status.get().getStatus().isCancelable();
    }

    public String getRecordingId() {
        return recordingId;
    }

    public String getBodyId() {
        return bodyId;
    }

    public String getSeriesTitle() {
        return seriesTitle;
    }

    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public Duration getDuration() {
        return duration;
    }

    private boolean hasSeasonAndEpisode() {
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

    @SuppressWarnings("unused")
    public RecordingState getState() {
        return state;
    }

    @SuppressWarnings("unused")
    public RecordingReason getReason() {
        return reason;
    }

    public boolean isCopyProtected() {
        return !isCopyable;
    }

    public boolean isSuggestion() {
        return reason == RecordingReason.TIVO_SUGGESTION;
    }

    public boolean isInProgress() {
        return state == RecordingState.IN_PROGRESS;
    }

    /**
     * Returns true if this recording matches the originalAirDate, or there was no original air date.
     */
    public boolean isOriginalRecording() {
        assert (dateRecorded != null);
        assert (dateRecorded.getValue() != null);

        return originalAirDate == null || (originalAirDate.getYear() == dateRecorded.getValue().getYear() &&
                originalAirDate.getDayOfYear() == dateRecorded.getValue().getDayOfYear());
    }

    public void isChildRecording(boolean val) {
        isChildRecording = val;
        title.setValue(buildTitle());
    }

    public int getNumEpisodes() {
        return numEpisodes;
    }

    public Path getDestination() {
        return destination;
    }

    public void setDestination(Path val) {
        destination = val;
    }

    public FileType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(FileType type) {
        destinationType = type;
    }

    public LocalDateTime getExpectedDeletion() {
        return expectedDeletion;
    }

    public String getFullTitle() {
        return buildSingleRecordingTitle();
    }

    public String getDefaultFilename() {
        return buildDefaultFilename();
    }

    /**
     * Build a sensible default filename that includes as much relevant available information as possible.
     */
    private String buildDefaultFilename() {
        StringJoiner components = new StringJoiner(" - ");
        int numComponents = 1; // We always add a title

        if (seriesTitle != null && !seriesTitle.isEmpty()) {
            components.add(seriesTitle);
        } else {
            // Ensure we have *something* for the title
            components.add(UNTITLED_TEXT);
        }

        if (hasSeasonAndEpisode()) {
            components.add(String.format("S%02dE%s", seriesNumber, getEpisodeNumberRange()));
            numComponents++;
        }

        if (episodeTitle != null && !episodeTitle.isEmpty()) {
            components.add(episodeTitle);
            numComponents++;
        }

        // If we only have a title and this isn't a movie, add the original air date.
        // If that doesn't exist, use the recording date.
        if (numComponents == 1 && collectionType != Type.MOVIE) {
            if (originalAirDate != null) {
                components.add(originalAirDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            } else {
                components.add(getDateRecorded().format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        }


        return filterUnsafeChars(components.toString());
    }

    private String getEpisodeNumberRange() {
        if (episodeNumbers.size() == 1) {
            return String.format("%02d", episodeNumbers.get(0));
        } else {
            return episodeNumbers.stream().map(Object::toString).collect(Collectors.joining(","));
        }
    }

    /**
     * Remove invalid filename characters from @filename.
     * Replace invalid characters with a space if they are between to two valid characters.
     */
    private String filterUnsafeChars(String filename) {
        StringBuilder sb = new StringBuilder();
        boolean prevCharIsSpace = false;
        for (Character c : filename.toCharArray()) {
            int type = Character.getType(c);
            if (Character.isLetterOrDigit(c) || c == '\'' || c == '.' || c == '&' || c == ',' || c == '!' ||
                    type == Character.DASH_PUNCTUATION) {
                sb.append(c);
                prevCharIsSpace = false;
            } else if (type != Character.OTHER_PUNCTUATION) {
                // Don't even add a space for OTHER_PUNCTUATION
                if (Character.isWhitespace(c) && !prevCharIsSpace) {
                    sb.append(c);
                    prevCharIsSpace = true;
                } else if (!prevCharIsSpace) {
                    // Replace an invalid character with a space
                    sb.append(' ');
                    prevCharIsSpace = true;
                }
            }
        }
        return sb.toString();
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public LocalDateTime getDateRecorded() {
        return dateRecorded.get();
    }

    public ObjectProperty<LocalDateTime> dateRecordedProperty() {
        return dateRecorded;
    }

    public ArchiveStatus getStatus() {
        return status.get();
    }

    public void setStatus(ArchiveStatus status) {
        this.status.setValue(status);
        updateIsArchivable();
        updateIsCancellable();
        updateIsPlayable();
    }

    public ObjectProperty<ArchiveStatus> statusProperty() {
        return status;
    }

    public boolean isSeriesHeading() {
        return isSeriesHeading.get();
    }

    @SuppressWarnings("unused")
    public BooleanProperty seriesHeadingProperty() {
        return isSeriesHeading;
    }

    public BooleanProperty isArchivableProperty() {
        return isArchivable;
    }

    public BooleanProperty isCancellableProperty() {
        return isCancellable;
    }

    public BooleanProperty isPlayableProperty() {
        return isPlayable;
    }

    public BooleanProperty isRemovableProperty() {
        return isRemovable;
    }

    private void updateIsArchivable() {
        boolean isArchivable = isArchivable();
        if (this.isArchivable.get() != isArchivable) {
            this.isArchivable.set(isArchivable);
        }
    }

    private void updateIsCancellable() {
        boolean isCancellable = !isSeriesHeading.get() && status.getValue().getStatus().isCancelable();
        if (this.isCancellable.get() != isCancellable) {
            this.isCancellable.set(isCancellable);
        }
    }

    private void updateIsPlayable() {
        boolean isPlayable = !isSeriesHeading.get() && status.getValue().getStatus() == ArchiveStatus.TaskStatus.FINISHED;
        if (this.isPlayable.get() != isPlayable) {
            this.isPlayable.set(isPlayable);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Recording recording = (Recording) o;

        return !(recordingId != null ? !recordingId.equals(recording.recordingId) : recording.recordingId != null) &&
                !(seriesTitle != null ? !seriesTitle.equals(recording.seriesTitle) : recording.seriesTitle != null);
    }

    @Override
    public int hashCode() {
        int result = recordingId != null ? recordingId.hashCode() : 0;
        result = 31 * result + (seriesTitle != null ? seriesTitle.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Recording{" +
                "title=" + title +
                ", dateRecorded=" + dateRecorded +
                ", status=" + status +
                ", isSeriesHeading=" + isSeriesHeading +
                ", isArchivable=" + isArchivable +
                ", isCancellable=" + isCancellable +
                ", isPlayable=" + isPlayable +
                ", isRemovable=" + isRemovable +
                ", recordingId='" + recordingId + '\'' +
                ", bodyId='" + bodyId + '\'' +
                ", seriesTitle='" + seriesTitle + '\'' +
                ", episodeTitle='" + episodeTitle + '\'' +
                ", duration=" + duration +
                ", seriesNumber=" + seriesNumber +
                ", episodeNumbers=" + episodeNumbers +
                ", channel=" + channel +
                ", description='" + description + '\'' +
                ", imageURL=" + imageURL +
                ", originalAirDate=" + originalAirDate +
                ", state=" + state +
                ", reason=" + reason +
                ", isCopyable=" + isCopyable +
                ", expectedDeletion=" + expectedDeletion +
                ", collectionType=" + collectionType +
                ", isChildRecording=" + isChildRecording +
                ", numEpisodes=" + numEpisodes +
                ", seasonAndEpisode='" + seasonAndEpisode + '\'' +
                ", destination=" + destination +
                ", destinationType=" + destinationType +
                '}';
    }

    public enum Type {
        SERIES,
        MOVIE;

        public static Type from(String val) {
            switch (val) {
                case "movie":
                    return MOVIE;
                default:
                    return SERIES;
            }
        }
    }

    public static class Builder {
        private String recordingId;
        private String bodyId;
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
        private boolean isChildRecording;
        private int numEpisodes;
        private LocalDateTime expectedDeletion;
        private Type collectionType;

        public Builder() {
            // Set default values
            episodeNumbers = Collections.emptyList();
            description = "No description available";
            state = RecordingState.UNKNOWN;
            reason = RecordingReason.UNKNOWN;
        }

        public Builder recordingId(String val) {
            recordingId = val;
            return this;
        }

        public Builder bodyId(String val) {
            bodyId = val;
            return this;
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

        @SuppressWarnings("unused")
        public Builder channel(String name, String number, URL logoURL) {
            channel = new Channel(name, number, logoURL);
            return this;
        }

        public Builder channel(Channel val) {
            channel = val;
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

        @SuppressWarnings("unused")
        public Builder isChildRecording(boolean val) {
            isChildRecording = val;
            return this;
        }

        public Builder numEpisodes(int val) {
            numEpisodes = val;
            return this;
        }

        public Builder expectedDeletion(LocalDateTime val) {
            expectedDeletion = val;
            return this;
        }

        public Builder collectionType(String val) {
            collectionType = Type.from(val);
            return this;
        }

        public Recording build() {
            return new Recording(this);
        }
    }
}
