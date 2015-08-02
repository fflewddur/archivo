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

package net.straylightlabs.archivo.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class DateUtils {
    private final static DateTimeFormatter DATE_RECORDED_LONG_DATE_FORMATTER;
    private final static DateTimeFormatter DATE_RECORDED_SHORT_DATE_FORMATTER;
    private final static DateTimeFormatter DATE_RECORDED_TIME_FORMATTER;
    public final static DateTimeFormatter DATE_AIRED_FORMATTER;

    private static int currentYear;
    private static int currentDay;
    private static int yesterday;

    static {
        DATE_RECORDED_LONG_DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        DATE_RECORDED_SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d");
        DATE_RECORDED_TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        DATE_AIRED_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        currentYear = LocalDateTime.now().getYear();
        currentDay = LocalDateTime.now().getDayOfYear();
        yesterday = LocalDateTime.now().minusDays(1).getDayOfYear();
    }

    public static String formatRecordedOnDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Recorded ");

        if (dateTime.getDayOfYear() == currentDay) {
            sb.append("today");
        } else if (dateTime.getDayOfYear() == yesterday) {
            sb.append("yesterday");
        } else if (dateTime.getYear() == currentYear) {
            // Don't include the year for recordings from the current year
            sb.append(dateTime.format(DATE_RECORDED_SHORT_DATE_FORMATTER));
        } else {
            sb.append(dateTime.format(DATE_RECORDED_LONG_DATE_FORMATTER));
        }

        sb.append(" at ");
        sb.append(dateTime.format(DATE_RECORDED_TIME_FORMATTER));

        return sb.toString();
    }

    public static String formatRecordedOnDate(LocalDateTime dateTime) {
        if (dateTime.getDayOfYear() == currentDay) {
            return "Today";
        } else if (dateTime.getDayOfYear() == yesterday) {
            return "Yesterday";
        } else if (dateTime.getYear() == currentYear) {
            // Don't include the year for recordings from the current year
            return dateTime.format(DATE_RECORDED_SHORT_DATE_FORMATTER);
        } else {
            return dateTime.format(DATE_RECORDED_LONG_DATE_FORMATTER);
        }
    }
}
