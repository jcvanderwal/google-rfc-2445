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

import com.google.ical.iter.RecurrenceIterable;
import com.google.ical.iter.RecurrenceIterator;
import com.google.ical.iter.RecurrenceIteratorFactory;
import com.google.ical.util.TimeUtils;
import com.google.ical.values.DateValue;
import com.google.ical.values.DateValueImpl;
import java.text.ParseException;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

/**
 * a factory for converting RRULEs and RDATEs into
 * <code>Iterator&lt;LocalDate&gt;</code> and
 * <code>Iterable&lt;LocalDate&gt;</code>.
 *
 * @see RecurrenceIteratorFactory
 *
 * @author mikesamuel+svn@gmail.com (Mike Samuel)
 */
public class LocalDateIteratorFactory {

  /**
   * given a block of RRULE, EXRULE, RDATE, and EXDATE content lines, parse
   * them into a single recurrence iterator.
   * @param strict true if any failure to parse should result in a
   *   ParseException.  false causes bad content lines to be logged and ignored.
   */
  public static LocalDateIterator createLocalLocalDateIterator(
      String rdata, LocalDate start, DateTimeZone tzid, boolean strict)
      throws ParseException {
    return new RecurrenceIteratorWrapper(
        RecurrenceIteratorFactory.createRecurrenceIterator(
            rdata, localDateToDateValue(start),
            tzid.toTimeZone(), strict));
  }

  public static LocalDateIterable createLocalDateIterable(
      String rdata, LocalDate start, DateTimeZone tzid, boolean strict)
      throws ParseException {
    return new RecurrenceIterableWrapper(
        RecurrenceIteratorFactory.createRecurrenceIterable(
            rdata, localDateToDateValue(start),
            tzid.toTimeZone(), strict));
  }

  public static LocalDateIterator createLocalDateIterator(
      RecurrenceIterator rit) {
    return new RecurrenceIteratorWrapper(rit);
  }

  private static final class RecurrenceIterableWrapper
      implements LocalDateIterable {
    private final RecurrenceIterable it;

    public RecurrenceIterableWrapper(RecurrenceIterable it) { this.it = it; }

    public LocalDateIterator iterator() {
      return new RecurrenceIteratorWrapper(it.iterator());
    }
  }

  private static final class RecurrenceIteratorWrapper
      implements LocalDateIterator {
    private final RecurrenceIterator it;
    RecurrenceIteratorWrapper(RecurrenceIterator it) { this.it = it; }
    public boolean hasNext() { return it.hasNext(); }
    public LocalDate next() { return dateValueToLocalDate(it.next()); }
    public void remove() { throw new UnsupportedOperationException(); }
    public void advanceTo(LocalDate d) {
      // we need to treat midnight as a date value so that passing in
      // dateValueToDate(<some-date-value>) will not advance past any
      // occurrences of some-date-value in the iterator.
      it.advanceTo(localDateToDateValue(d));
    }
  }

  private static final long MILLIS_PER_SECOND = 1000;
  static LocalDate dateValueToLocalDate(DateValue dvUtc) {
    return new LocalDate(dvUtc.year(), dvUtc.month(), dvUtc.day());
  }

  static DateValue localDateToDateValue(LocalDate date) {
    return new DateValueImpl(
        date.getYear(), date.getMonthOfYear(), date.getDayOfMonth());
  }

  private LocalDateIteratorFactory() {
    // uninstantiable
  }
}
