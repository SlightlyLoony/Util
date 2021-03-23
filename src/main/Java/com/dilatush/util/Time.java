package com.dilatush.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Static container class for utility functions related to time.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
@SuppressWarnings( "unused" )
public class Time {

    /* Convenience definitions of some common date and time formatters */

    /** Format a date like 3/9/21 (month/day/year) */
    public static final DateTimeFormatter US_SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern( "M/d/y" );

    /** Format a date like 03/09/2021 (month/day/year) */
    public static final DateTimeFormatter US_STANDARD_DATE_FORMAT = DateTimeFormatter.ofPattern( "MM/dd/yyyy" );

    /** Format a date like 03-09-2021 (month-day-year) */
    public static final DateTimeFormatter US_STANDARD_DATE_FORMAT_HYPHEN = DateTimeFormatter.ofPattern( "MM-dd-yyyy" );

    /** Format a date like 20210314 (year month day) */
    public static final DateTimeFormatter ORDERED_DATE_FORMAT = DateTimeFormatter.ofPattern( "yyyyMMdd" );

    /** Format a date like 2021-03-14 (year-month-day) */
    public static final DateTimeFormatter ORDERED_DATE_FORMAT_HYPHEN = DateTimeFormatter.ofPattern( "yyyy-MM-dd" );

    /** Format a date like Mar 4, 2021 */
    public static final DateTimeFormatter US_DATE_FORMAT = DateTimeFormatter.ofPattern( "MMM d, yyyy" );

    /** Format a date like March 4, 2021 */
    public static final DateTimeFormatter US_FORMAL_DATE_FORMAT = DateTimeFormatter.ofPattern( "MMMM d, yyyy" );

    /** Format a time like 14:03:58 (24 hour clock, one second resolution) */
    public static final DateTimeFormatter TIME_FORMAT_24 = DateTimeFormatter.ofPattern( "HH:mm:ss" );

    /** Format a time like 14:03:58 (24 hour clock, one millisecond resolution) */
    public static final DateTimeFormatter TIME_FORMAT_24_MS = DateTimeFormatter.ofPattern( "HH:mm:ss.SSS" );

    /** Format a time like 02:03:58 PM (12 hour clock with AM/PM, one second resolution) */
    public static final DateTimeFormatter TIME_FORMAT_12 = DateTimeFormatter.ofPattern( "hh:mm:ss a" );

    /** Format a time like 020358 (24 hour clock, one second resolution) */
    public static final DateTimeFormatter ORDERED_TIME_FORMAT = DateTimeFormatter.ofPattern( "HHmmss" );

    /** Format a time like 02-03-58 (24 hour clock, one second resolution) */
    public static final DateTimeFormatter ORDERED_TIME_FORMAT_HYPHEN = DateTimeFormatter.ofPattern( "HH-mm-ss" );

    /** Format a datetime like 3/9/21 14:03:58 (month/day/year hour:minute:second) */
    public static final DateTimeFormatter US_SHORT_DATETIME_FORMAT_24 = DateTimeFormatter.ofPattern( "M/d/y HH:mm:ss" );

    /** Format a datetime like 3/9/21 02:03:58 PM (month/day/year hour:minute:second AM/PM) */
    public static final DateTimeFormatter US_SHORT_DATETIME_FORMAT_12 = DateTimeFormatter.ofPattern( "M/d/y hh:mm:ss a" );

    /** Format a datetime like Mar 9, 2021 14:03:58 (month/day/year hour:minute:second) */
    public static final DateTimeFormatter US_STANDARD_DATETIME_FORMAT_24 = DateTimeFormatter.ofPattern( "MMM d, yyyy HH:mm:ss" );

    /** Format a datetime like Mar 9, 2021 02:03:58 PM (month/day/year hour:minute:second AM/PM) */
    public static final DateTimeFormatter US_STANDARD_DATETIME_FORMAT_12 = DateTimeFormatter.ofPattern( "MMM d, yyyy hh:mm:ss a" );

    /** Format a datetime like March 9, 2021 14:03:58 (month/day/year hour:minute:second) */
    public static final DateTimeFormatter US_FORMAL_DATETIME_FORMAT_24 = DateTimeFormatter.ofPattern( "MMMM d, yyyy HH:mm:ss" );

    /** Format a datetime like March 9, 2021 02:03:58 PM (month/day/year hour:minute:second AM/PM) */
    public static final DateTimeFormatter US_FORMAL_DATETIME_FORMAT_12 = DateTimeFormatter.ofPattern( "MMMM d, yyyy hh:mm:ss a" );

    /** Format a datetime like 20210314140358 (year month day hour minute second) */
    public static final DateTimeFormatter ORDERED_DATETIME_FORMAT = DateTimeFormatter.ofPattern( "yyyyMMddHHmmss" );

    /** Format a datetime like 2021-03-14-14-03-58 (year month day hour minute second) */
    public static final DateTimeFormatter ORDERED_DATETIME_FORMAT_HYPHEN = DateTimeFormatter.ofPattern( "yyyy-MM-dd-HH-mm-ss" );


    /* Convenience definitions of all the United States time zones */

    /** Pacific Daylight Time */
    public static final ZoneId PDT  = ZoneId.of( "America/Los_Angeles" );

    /** Mountain Daylight Time */
    public static final ZoneId MDT  = ZoneId.of( "America/Denver"      );

    /** Mountain Standard Time */
    public static final ZoneId MST  = ZoneId.of( "America/Phoenix"     );

    /** Central Daylight Time */
    public static final ZoneId CDT  = ZoneId.of( "America/Chicago"     );

    /** Eastern Daylight Time */
    public static final ZoneId EDT  = ZoneId.of( "America/New_York"    );

    /** Alaska Daylight Time */
    public static final ZoneId AKDT = ZoneId.of( "America/Anchorage"   );

    /** Hawaiian Daylight Time (Aleutian Isles) */
    public static final ZoneId HADT = ZoneId.of( "America/Adak"        );

    /** Hawaiian Standard Time (Hawaii) */
    public static final ZoneId HAST = ZoneId.of( "Pacific/Honolulu"    );


    /**
     * Returns a {@link ZonedDateTime} instance representing the same time as the given {@link Instant}, but in the time zone specified by the
     * given {@link ZoneId}.
     *
     * @param _instant The {@link Instant} to create a {@link ZonedDateTime} from.
     * @param _zoneId The {@link ZoneId} specifying the time zone for the resultant {@link ZonedDateTime}.
     * @return the {@link ZonedDateTime} created from the given instant and time zone
     */
    public static ZonedDateTime fromInstant( final Instant _instant, final ZoneId _zoneId ) {
        return ZonedDateTime.ofInstant( _instant, _zoneId );
    }
}
