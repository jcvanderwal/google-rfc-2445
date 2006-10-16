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

package com.google.ical.compat.javautil;

import com.google.ical.iter.RecurrenceIterator;
import com.google.ical.iter.RecurrenceIteratorFactory;
import com.google.ical.util.TimeUtils;
import com.google.ical.values.DateTimeValueImpl;
import com.google.ical.values.DateValue;
import com.google.ical.values.DateValueImpl;
import com.google.ical.values.TimeValue;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * a factory for converting RRULEs and RDATEs into
 * <code>Iterator&lt;Date&gt;</code> and <code>Iterable&lt;Date&gt;</code>.
 *
 * @author Mike Samuel (msamuel@google.com)
 */
public class DateIteratorFactory {



  private static final class RecurrenceIteratorWrapper
      implements DateIterator {
    private final RecurrenceIterator it;
    RecurrenceIteratorWrapper(RecurrenceIterator it) { this.it = it; }
    public boolean hasNext() { return it.hasNext(); }
    public Date next() { return dateValueToDate(it.next()); }
    public void remove() { throw new UnsupportedOperationException(); }
    public void advanceTo(Date d) {
      // we need to treat midnight as a date value so that passing in
      // dateValueToDate(<some-date-value>) will not advance past any
      // occurrences of some-date-value in the iterator.
      it.advanceTo(dateToDateValue(d, true));
    }
  }

  private static final long MILLIS_PER_SECOND = 1000;
  static Date dateValueToDate(DateValue dvUtc) {
    GregorianCalendar c = new GregorianCalendar(TimeUtils.utcTimezone());
    c.clear();
    if (dvUtc instanceof TimeValue) {
      TimeValue tvUtc = (TimeValue) dvUtc;
      c.set(dvUtc.year(),
            dvUtc.month() - 1,  // java.util's dates are zero-indexed
            dvUtc.day(),
            tvUtc.hour(),
            tvUtc.minute(),
            tvUtc.second());
    } else {
      c.set(dvUtc.year(),
            dvUtc.month() - 1,  // java.util's dates are zero-indexed
            dvUtc.day(),
            0,
            0,
            0);
    }
    return c.getTime();
  }

  static DateValue dateToDateValue(Date date, boolean midnightAsDate) {
    GregorianCalendar c = new GregorianCalendar(TimeUtils.utcTimezone());
    c.setTime(date);
    int h = c.get(Calendar.HOUR_OF_DAY),
      m = c.get(Calendar.MINUTE),
      s = c.get(Calendar.SECOND);
    if (midnightAsDate && 0 == (h | m | s)) {
      return new DateValueImpl(c.get(Calendar.YEAR),
                               c.get(Calendar.MONTH) + 1,
                               c.get(Calendar.DAY_OF_MONTH));
    } else {
      return new DateTimeValueImpl(c.get(Calendar.YEAR),
                                   c.get(Calendar.MONTH) + 1,
                                   c.get(Calendar.DAY_OF_MONTH),
                                   h,
                                   m,
                                   s);
    }
  }

  private DateIteratorFactory() {
    // uninstantiable
  }
}
