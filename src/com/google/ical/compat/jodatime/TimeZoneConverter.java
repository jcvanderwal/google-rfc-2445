// Copyright (C) 2006 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ical.compat.jodatime;

import java.util.GregorianCalendar;
import java.util.Date;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.tz.FixedDateTimeZone;

/**
 * Replacement for Joda-time's broken {@link DateTimeZone#toTimeZone} which
 * will return a {@link java.util.TimeZone}.  <code>java.util.TimeZones</code>
 * should not be used since they're frequently not up-to-date re Brazilian
 * timezones.
 *
 * @author mikesamuel+svn@gmail.com (Mike Samuel)
 */
final class TimeZoneConverter {

  static final int MILLISECONDS_PER_SECOND = 1000;
  static final int MILLISECONDS_PER_MINUTE = 60 * MILLISECONDS_PER_SECOND;
  static final int MILLISECONDS_PER_HOUR = 60 * MILLISECONDS_PER_MINUTE;

  static TimeZone toTimeZone(final DateTimeZone dtz) {
    if (dtz.isFixed()) { return dtz.toTimeZone(); }  // efficient for UTC

    TimeZone tz = new TimeZone() {
        public void setRawOffset(int n) {
          throw new UnsupportedOperationException();
        }
        public boolean useDaylightTime() {
          return !dtz.isFixed();
        }
        public boolean inDaylightTime(Date d) {
          long t = d.getTime();
          return dtz.getStandardOffset(t) != dtz.getOffset(t);
        }
        public int getRawOffset() {
          return dtz.getStandardOffset(0);
        }
        public int getOffset(long instant) {
          return dtz.getOffset(instant);
        }
        public int getOffset(
            int era, int year, int month, int day, int dayOfWeek,
            int milliseconds) {
          int millis = milliseconds;  // milliseconds is day in standard time
          int hour = millis / MILLISECONDS_PER_HOUR;
          millis %= MILLISECONDS_PER_HOUR;
          int minute = millis / MILLISECONDS_PER_MINUTE;
          millis %= MILLISECONDS_PER_MINUTE;
          int second = millis / MILLISECONDS_PER_SECOND;
          millis %= MILLISECONDS_PER_SECOND;
          if (era == GregorianCalendar.BC) { year = -(year - 1); }

          DateTime dt = new DateTime(year, month + 1, day, hour, minute,
                                     second, millis, dtz);
          int offset = dtz.getStandardOffset(dt.getMillis());
          DateTime stdDt = new DateTime(year, month + 1, day, hour, minute,
                                        second, millis, standardTz(offset));
          return getOffset(stdDt.getMillis());
        }
      };
    tz.setID(dtz.getID());
    return tz;
  }

  private static FixedDateTimeZone[] dtzCache;
  private static FixedDateTimeZone standardTz(int offset) {
    if (0 == (offset % (30 * MILLISECONDS_PER_MINUTE))) {
      int i = (offset / (30 * MILLISECONDS_PER_MINUTE)) + 50;
      if (i >= 0 && i <= 100) {
        if (null == dtzCache) { dtzCache = new FixedDateTimeZone[100]; }
        if (null == dtzCache[i]) {
          dtzCache[i] = makeStandardTz(offset);
        }
        return dtzCache[i];
      }
    }
    return makeStandardTz(offset);
  }

  private static FixedDateTimeZone makeStandardTz(int offset) {
    int absOffset = Math.abs(offset);
    String id = String.format(
        "GMT%c%02d%02d",
        offset < 0 ? '-' : '+',
        absOffset / MILLISECONDS_PER_HOUR,
        (absOffset % MILLISECONDS_PER_HOUR) / MILLISECONDS_PER_MINUTE);
    return new FixedDateTimeZone(id, id, offset, offset);
  }

  private TimeZoneConverter() {
    // uninstantiable
  }

}
