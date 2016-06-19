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

package net.straylightlabs.archivo.view;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import net.straylightlabs.archivo.model.Recording;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Model the group of currently selected recordings.
 */
public class RecordingSelection {
    private final List<Recording> recordings;
    private final List<Recording> recordingsWithChildren;

    // Properties to enable/disable UI controls
    private final BooleanProperty showControls;
    private final BooleanProperty isArchivable;
    private final BooleanProperty isCancellable;
    private final BooleanProperty isPlayable;
    private final BooleanProperty isRemovable;
    private final BooleanAndBinding archivableBinding;
    private final BooleanAndBinding cancellableBinding;
    private final BooleanAndBinding playableBinding;
    private final BooleanAndBinding removableBinding;

    // Properties for displaying recording information
    private final StringProperty showTitle;
    private final StringProperty episodeTitle;
    private final StringProperty description;
    private final StringProperty episodeNumber;
    private final StringProperty dateRecorded;
    private final StringProperty dateFirstAired;
    private final StringProperty duration;
    private final StringProperty channel;
    private final ObjectProperty<LocalDateTime> expectedRemovalDate;
    private final BooleanProperty isRecording;
    private final BooleanProperty isCopyProtected;
    private final ObjectProperty<URL> posterURL;

    private final Map<String, Integer> showTitles;

    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(RecordingSelection.class);

    public RecordingSelection() {
        recordings = new ArrayList<>();
        recordingsWithChildren = new ArrayList<>();

        showControls = new SimpleBooleanProperty();
        isArchivable = new SimpleBooleanProperty();
        isCancellable = new SimpleBooleanProperty();
        isPlayable = new SimpleBooleanProperty();
        isRemovable = new SimpleBooleanProperty();
        archivableBinding = new BooleanAndBinding();
        cancellableBinding = new BooleanAndBinding();
        playableBinding = new BooleanAndBinding();
        removableBinding = new BooleanAndBinding();

        showTitle = new SimpleStringProperty();
        episodeTitle = new SimpleStringProperty();
        description = new SimpleStringProperty();
        episodeNumber = new SimpleStringProperty();
        dateRecorded = new SimpleStringProperty();
        dateFirstAired = new SimpleStringProperty();
        duration = new SimpleStringProperty();
        channel = new SimpleStringProperty();
        expectedRemovalDate = new SimpleObjectProperty<>();
        isRecording = new SimpleBooleanProperty();
        isCopyProtected = new SimpleBooleanProperty();
        posterURL = new SimpleObjectProperty<>();

        showTitles = new HashMap<>();
    }

    /**
     * Return a List of the selected Recordings.
     *
     * @return List of Recordings
     */
    public List<Recording> getRecordings() {
        return Collections.unmodifiableList(recordings);
    }

    /**
     * Return a List of the selected Recordings, including any child recordings of series headers.
     *
     * @return List of Recordings
     */
    public List<Recording> getRecordingsWithChildren() {
        return Collections.unmodifiableList(recordingsWithChildren);
    }

    public void selectionChanged(ListChangeListener.Change<? extends TreeItem<Recording>> change) {
        logger.debug("Selection changed");

        ObservableList<? extends TreeItem<Recording>> selectedRecordings = change.getList();

        clearFields();
        boolean isAnyRecordingCopyProtected = false;
        boolean isAnyRecordingInProgress = false;

        Recording lastRecording = null;
        for (TreeItem<Recording> item : selectedRecordings) {
            if (item == null) {
                continue;
            }
            Recording recording = item.getValue();
            if (recording == null) {
                continue;
            }

            recordings.add(recording);
            recordingsWithChildren.add(recording);
            recordingsWithChildren.addAll(item.getChildren().stream().map(TreeItem::getValue).collect(Collectors.toList()));

            lastRecording = recording;
            int episodes = showTitles.getOrDefault(recording.getSeriesTitle(), 0);
            showTitles.put(recording.getSeriesTitle(), episodes + 1);
            archivableBinding.add(recording.isArchivableProperty());
            cancellableBinding.add(recording.isCancellableProperty());
            playableBinding.add(recording.isPlayableProperty());
            removableBinding.add(recording.isRemovableProperty());
            if (recording.isCopyProtected()) {
                isAnyRecordingCopyProtected = true;
            }
            if (recording.isInProgress()) {
                isAnyRecordingInProgress = true;
            }
        }

        rebindProperties();
        clearLabelProperties();
        updateLabelProperties(
                lastRecording, selectedRecordings.size(), isAnyRecordingCopyProtected, isAnyRecordingInProgress
        );
        updateControlProperties(selectedRecordings.size());
    }

    private void clearFields() {
        archivableBinding.clear();
        cancellableBinding.clear();
        playableBinding.clear();
        removableBinding.clear();
        showTitles.clear();
        recordings.clear();
        recordingsWithChildren.clear();
    }

    private void rebindProperties() {
        isArchivable.unbind();
        isCancellable.unbind();
        isPlayable.unbind();
        isRemovable.unbind();

        isArchivable.bind(archivableBinding);
        isCancellable.bind(cancellableBinding);
        isPlayable.bind(playableBinding);
        isRemovable.bind(removableBinding);
    }

    private void updateLabelProperties(Recording lastRecording, int numRecordings, boolean isAnyRecordingCopyProtected,
                                       boolean isAnyRecordingInProgress) {
        clearLabelProperties();

        if (numRecordings == 1 && lastRecording != null) {
            // Only one recording is selected, show all of its details
            showTitle.setValue(lastRecording.getSeriesTitle());
            if (lastRecording.isSeriesHeading()) {
                setSeriesOverviewValues(lastRecording);
            } else {
                setRecordingValues(lastRecording);
            }
        } else if (showTitles.size() == 1 && lastRecording != null) {
            // Multiple episodes from one series are selected
            showTitle.setValue(lastRecording.getSeriesTitle());
            posterURL.setValue(lastRecording.getImageURL());
            episodeTitle.setValue(String.format("%d episodes selected", showTitles.get(lastRecording.getSeriesTitle())));
            isCopyProtected.setValue(isAnyRecordingCopyProtected);
            isRecording.setValue(isAnyRecordingInProgress);
        } else if (numRecordings > 0) {
            // Multiple episodes from multiple series are selected
            showTitle.setValue(String.format("%d shows selected", showTitles.size()));
            int numEpisodes = showTitles.keySet().stream().mapToInt(showTitles::get).sum();
            episodeTitle.setValue(String.format("%d episodes selected", numEpisodes));
            isCopyProtected.setValue(isAnyRecordingCopyProtected);
            isRecording.setValue(isAnyRecordingInProgress);
        }
    }

    private void clearLabelProperties() {
        showTitle.setValue("");
        episodeTitle.setValue("");
        description.setValue("");
        episodeNumber.setValue("");
        dateRecorded.setValue("");
        dateFirstAired.setValue("");
        duration.setValue("");
        channel.setValue("");
        expectedRemovalDate.setValue(null);
        isCopyProtected.setValue(false);
        isRecording.setValue(false);
        posterURL.setValue(null);
    }

    private void setSeriesOverviewValues(Recording recording) {
        int numEpisodes = recording.getNumEpisodes();
        if (numEpisodes == 1) {
            episodeTitle.setValue(String.format("%d episode", numEpisodes));
        } else if (numEpisodes > 1) {
            episodeTitle.setValue(String.format("%d episodes", numEpisodes));
        }
        posterURL.setValue(recording.getImageURL());
    }

    private void setRecordingValues(Recording recording) {
        episodeTitle.setValue(recording.getEpisodeTitle());
        description.setValue(recording.getDescription());
        episodeNumber.setValue(recording.getSeasonAndEpisode());
        dateRecorded.setValue(DateUtils.formatRecordedOnDateTime(recording.getDateRecorded()));
        if (!recording.isOriginalRecording()) {
            dateFirstAired.setValue(
                    String.format("Originally aired on %s", recording.getOriginalAirDate().format(DateUtils.DATE_AIRED_FORMATTER))
            );
        }
        if (recording.getDuration() != null) {
            duration.setValue(DateUtils.formatDuration(recording.getDuration(), recording.isInProgress()));
        }
        if (recording.getChannel() != null) {
            channel.setValue(String.format(
                    "Channel %s (%s)", recording.getChannel().getNumber(), recording.getChannel().getName()
            ));
        }
        expectedRemovalDate.setValue(recording.getExpectedDeletion());
        isRecording.setValue(recording.isInProgress());
        isCopyProtected.setValue(recording.isCopyProtected());
        posterURL.setValue(recording.getImageURL());
    }

    private void updateControlProperties(int numRecordings) {
        showControls.setValue(numRecordings > 0);
    }

    public BooleanProperty showControlsProperty() {
        return showControls;
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

    public StringProperty showTitleProperty() {
        return showTitle;
    }

    public StringProperty episodeTitleProperty() {
        return episodeTitle;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty episodeNumberProperty() {
        return episodeNumber;
    }

    public StringProperty dateRecordedProperty() {
        return dateRecorded;
    }

    public StringProperty dateFirstAiredProperty() {
        return dateFirstAired;
    }

    public StringProperty durationProperty() {
        return duration;
    }

    public StringProperty channelProperty() {
        return channel;
    }

    public ObjectProperty<LocalDateTime> expectedRemovalDateProperty() {
        return expectedRemovalDate;
    }

    public BooleanProperty isRecordingProperty() {
        return isRecording;
    }

    public BooleanProperty isCopyProtectedProperty() {
        return isCopyProtected;
    }

    public ObjectProperty<URL> posterUrlProperty() {
        return posterURL;
    }

    class BooleanAndBinding extends BooleanBinding {
        private final List<BooleanProperty> properties;

        public BooleanAndBinding() {
            super();
            properties = new ArrayList<>();
        }

        public void add(BooleanProperty property) {
            properties.add(property);
            bind(property);
            invalidate();
        }

        public void clear() {
            properties.forEach(this::unbind);
            properties.clear();
            invalidate();
        }

        @Override
        protected boolean computeValue() {
            // If there aren't any elements, return false
            if (properties.size() == 0) {
                return false;
            }
            // If any of the elements is false, return false
            for (BooleanProperty property : properties) {
                if (!property.get()) {
                    return false;
                }
            }
            // All of the elements are true
            return true;
        }
    }
}
