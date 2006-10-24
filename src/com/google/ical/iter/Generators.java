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

package com.google.ical.iter;

import com.google.ical.util.DTBuilder;
import com.google.ical.util.TimeUtils;
import com.google.ical.values.DateValue;
import com.google.ical.values.DateValueImpl;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import java.util.Arrays;

/**
 * factory for field generators.
 *
 * @author mikesamuel+svn@gmail.com (Mike Samuel)
 */
final class Generators {

  /**
   * the maximum number of years generated between instances.
   * See {@link ThrottledGenerator} for a description of the problem this
   * solves.
   * Note: this counts the maximum number of years generated, so for
   * FREQ=YEARLY;INTERVAL=4 the generator would try 100 individual years over
   * a span of 400 years before giving up and concluding that the rule generates
   * no usable dates.
   */
  private static final int MAX_YEARS_BETWEEN_INSTANCES = 100;

  /**
   * constructs a generator that generates years successively counting from the
   * first year passed in.
   * @param interval number of years to advance each step.
   * @param dtStart non null
   * @return the year in dtStart the first time called and interval + last
   *   return value on subsequent calls.
   */
  static ThrottledGenerator serialYearGenerator(
      final int interval, final DateValue dtStart) {
    return new ThrottledGenerator() {
        /** the last year seen */
        int year = dtStart.year() - interval;
        int throttle = MAX_YEARS_BETWEEN_INSTANCES;

        @Override
        boolean generate(DTBuilder builder)
            throws IteratorShortCircuitingException {
          // make sure things halt even if the rrule is bad.
          // Rules like
          //   FREQ=YEARLY;BYMONTHDAY=30;BYMONTH=2
          // should halt

          if (--throttle < 0) {
            throw IteratorShortCircuitingException.instance();
          }
          builder.year = year += interval;
          return true;
        }

        @Override
        void workDone() { this.throttle = MAX_YEARS_BETWEEN_INSTANCES; }

        @Override
        public String toString() { return "serialYearGenerator:" + interval; }
      };
  }

  /**
   * constructs a generator that generates months in the given builder's year
   * successively counting from the first month passed in.
   * @param interval number of months to advance each step.
   * @param dtStart non null.
   * @return the year in dtStart the first time called and interval + last
   *   return value on subsequent calls.
   */
  static Generator serialMonthGenerator(
      final int interval, final DateValue dtStart) {
    return new Generator() {
        int year = dtStart.year();
        int month = dtStart.month() - interval;
        {
          while (month < 1) {
            month += 12;
            --year;
          }
        }
        @Override
        boolean generate(DTBuilder builder) {
          int nmonth;
          if (year != builder.year) {
            int monthsBetween = (builder.year - year) * 12 - (month - 1);
            nmonth = ((interval - (monthsBetween % interval)) % interval) + 1;
            if (nmonth > 12) {
              // don't update year so that the difference calculation above is
              // correct when this function is reentered with a different year
              return false;
            }
            year = builder.year;
          } else {
            nmonth = month + interval;
            if (nmonth > 12) {
              return false;
            }
          }
          month = builder.month = nmonth;
          return true;
        }

        @Override
        public String toString() { return "serialMonthGenerator:" + interval; }
      };
  }

  /**
   * constructs a generator that generates every day in the current month that
   * is an integer multiple of interval days from dtStart.
   */
  static Generator serialDayGenerator(
      final int interval, final DateValue dtStart) {
    return new Generator() {
        int year, month, date;
        /** ndays in the last month encountered */
        int nDays;

        {
          // step back one interval
          DTBuilder dtStartMinus1B = new DTBuilder(dtStart);
          dtStartMinus1B.day -= interval;
          DateValue dtStartMinus1 = dtStartMinus1B.toDate();
          year = dtStartMinus1.year();
          month = dtStartMinus1.month();
          date = dtStartMinus1.day();
          nDays = TimeUtils.monthLength(year, month);
        }

        @Override
        boolean generate(DTBuilder builder) {
          int ndate;
          if (year == builder.year && month == builder.month) {
            ndate = date + interval;
            if (ndate > nDays) {
              return false;
            }
          } else {
            nDays = TimeUtils.monthLength(builder.year, builder.month);
            if (interval != 1) {
              // Calculate the number of days between the first of the new
              // month andthe old date and extend it to make it an integer
              // multiple of interval
              int daysBetween = TimeUtils.daysBetween(
                  new DateValueImpl(builder.year, builder.month, 1),
                  new DateValueImpl(year, month, date));
              ndate = ((interval - (daysBetween % interval)) % interval) + 1;
              if (ndate > nDays) {
                // need to early out without updating year or month so that the
                // next time we enter, with a different month, the daysBetween
                // call above compares against the proper last date
                return false;
              }
            } else {
              ndate = 1;
            }
            year = builder.year;
            month = builder.month;
          }
          date = builder.day = ndate;
          return true;
        }

        @Override
        public String toString() { return "serialDayGenerator:" + interval; }
      };
  }

  /**
   * constructs a generator that yields the specified years in increasing order.
   */
  static Generator byYearGenerator(int[] years, final DateValue dtStart) {
    final int[] uyears = Util.uniquify(years);

    // index into years
    return new Generator() {
        int i;
        {
          while (i < uyears.length && dtStart.year() > uyears[i]) { ++i; }
        }

        @Override
        boolean generate(DTBuilder builder) {
          if (i >= uyears.length) { return false; }
          builder.year = uyears[i++];
          return true;
        }

        @Override
        public String toString() { return "byYearGenerator"; }
      };
  }

  /**
   * constructs a generator that yields the specified months in increasing order
   * for each year.
   * @param months values in [1-12]
   * @param dtStart non null
   */
  static Generator byMonthGenerator(int[] months, final DateValue dtStart) {
    final int[] umonths = Util.uniquify(months);

    return new Generator() {
        int i;
        int year = dtStart.year();

        @Override
        boolean generate(DTBuilder builder) {
          if (year != builder.year) {
            i = 0;
            year = builder.year;
          }
          if (i >= umonths.length) { return false; }
          builder.month = umonths[i++];
          return true;
        }

        @Override
        public String toString() { return "byMonthGenerator"; }
      };
  }

  /**
   * constructs a function that yields the specified dates
   * (possibly relative to end of month) in increasing order
   * for each month seen.
   * @param dates elements in [-53,53] != 0
   * @param dtStart non null
   */
  static Generator byMonthDayGenerator(int[] dates, final DateValue dtStart) {
    final int[] udates = Util.uniquify(dates);

    return new Generator() {
        int year = dtStart.year();
        int month = dtStart.month();
        /** list of generated dates for the current month */
        int[] posDates;
        /** index of next date to return */
        int i = 0;

        {
          convertDatesToAbsolute();
        }

        private void convertDatesToAbsolute() {
          IntSet posDates = new IntSet();
          int nDays = TimeUtils.monthLength(year, month);
          for (int j = 0; j < udates.length; ++j) {
            int date = udates[j];
            if (date < 0) {
              date += nDays + 1;
            }
            if (date >= 1 && date <= nDays) {
              posDates.add(date);
            }
          }
          this.posDates = posDates.toIntArray();
        }

        @Override
        boolean generate(DTBuilder builder) {
          if (year != builder.year || month != builder.month) {
            year = builder.year;
            month = builder.month;

            convertDatesToAbsolute();

            i = 0;
          }
          if (i >= posDates.length) { return false; }
          builder.day = posDates[i++];
          return true;
        }

        @Override
        public String toString() { return "byMonthDayGenerator"; }
      };
  }

  /**
   * constructs a day generator based on a BYDAY rule.
   *
   * @param days day of week, number pairs,
   *   e.g. SU,3MO means every sunday and the 3rd monday.
   * @param weeksInYear are the week numbers meant to be weeks in the
   *   current year, or weeks in the current month.
   * @param dtStart non null
   */
  static Generator byDayGenerator(
      WeekdayNum[] days, final boolean weeksInYear, final DateValue dtStart) {
    final WeekdayNum[] udays = days.clone();

    return new Generator() {
        int year = dtStart.year();
        int month = dtStart.month();
        /** list of generated dates for the current month */
        int[] dates;
        /** index of next date to return */
        int i = 0;

        { generateDates(); }

        void generateDates() {
          int nDays;
          Weekday dow0;
          int nDaysInMonth = TimeUtils.monthLength(year, month);
          // index of the first day of the month in the month or year
          int d0;

          if (weeksInYear) {
            nDays = TimeUtils.yearLength(year);
            dow0 = Weekday.firstDayOfWeekInMonth(year, 1);
            d0 = TimeUtils.dayOfYear(year, month, 1);
          } else {
            nDays = nDaysInMonth;
            dow0 = Weekday.firstDayOfWeekInMonth(year, month);
            d0 = 0;
          }

          // an index not greater than the first week of the month in the month
          // or year
          int w0 = d0 / 7;

          // iterate through days and resolve each [week, day of week] pair to a
          // day of the month
          IntSet udates = new IntSet();
          for (int j = 0; j < udays.length; ++j) {
            WeekdayNum day = udays[j];
            if (0 != day.num) {
              int date = Util.dayNumToDate(
                  dow0, nDays, day.num, day.wday, d0, nDaysInMonth);
              if (0 != date) { udates.add(date); }
            } else {
              int wn = w0 + 6;
              for (int w = w0; w <= wn; ++w) {
                int date = Util.dayNumToDate(
                    dow0, nDays, w, day.wday, d0, nDaysInMonth);
                if (0 != date) { udates.add(date); }
              }
            }
          }
          dates = udates.toIntArray();
        }

        @Override
        boolean generate(DTBuilder builder) {
          if (year != builder.year || month != builder.month) {
            year = builder.year;
            month = builder.month;

            generateDates();
            // start at the beginning of the month
            i = 0;
          }
          if (i >= dates.length) { return false; }
          builder.day = dates[i++];
          return true;
        }

        @Override
        public String toString() {
          return "byDayGenerator:" + Arrays.toString(udays);
        }
      };
  }

  /**
   * constructs a generator that yields each day in the current month that falls
   * in one of the given weeks of the year.
   * @param weekNos (elements in [-53,53] != 0) week numbers
   * @param wkst (in RRULE_WDAY_*) day of the week that the week starts on.
   * @param dtStart non null
   */
  static Generator byWeekNoGenerator(
      int[] weekNos, final Weekday wkst, final DateValue dtStart) {
    final int[] uWeekNos = Util.uniquify(weekNos);

    return new Generator() {
        int year = dtStart.year();
        int month = dtStart.month();
        /** number of weeks in the last year seen */
        int weeksInYear;
        /** dates generated anew for each month seen */
        int[] dates;
        /** index into dates */
        int i = 0;

        /**
         * day of the year of the start of week 1 of the current year.
         * Since week 1 may start on the previous year, this may be negative.
         */
        int doyOfStartOfWeek1;

        {
          checkYear();
          checkMonth();
        }

        void checkYear() {
          // if the first day of jan is wkst, then there are 7.
          // if the first day of jan is wkst + 1, then there are 6
          // if the first day of jan is wkst + 6, then there is 1
          Weekday dowJan1 = Weekday.firstDayOfWeekInMonth(year, 1);
          int nDaysInFirstWeek =
            7 - ((7 + dowJan1.javaDayNum - wkst.javaDayNum) % 7);
          // number of days not in any week
          int nOrphanedDays = 0;
          // according to RFC 2445
          //     Week number one of the calendar year is the first week which
          //     contains at least four (4) days in that calendar year.
          if (nDaysInFirstWeek < 4) {
            nOrphanedDays = nDaysInFirstWeek;
            nDaysInFirstWeek = 7;
          }

          // calculate the day of year (possibly negative) of the start of the
          // first week in the year.  This day must be of wkst.
          doyOfStartOfWeek1 = nDaysInFirstWeek - 7 + nOrphanedDays;

          weeksInYear = (TimeUtils.yearLength(year) - nOrphanedDays + 6) / 7;
        }

        void checkMonth() {
          // the day of the year of the 1st day in the month
          int doyOfMonth1 = TimeUtils.dayOfYear(year, month, 1);
          // the week of the year of the 1st day of the month.  approximate.
          int weekOfMonth = ((doyOfMonth1 - doyOfStartOfWeek1) / 7) + 1;
          // number of days in the month
          int nDays = TimeUtils.monthLength(year, month);

          // generate the dates in the month
          IntSet udates = new IntSet();
          for (int j = 0; j < uWeekNos.length; j++) {
            int weekNo = uWeekNos[j];
            if (weekNo < 0) {
              weekNo += weeksInYear + 1;
            }
            if (weekNo >= weekOfMonth - 1 && weekNo <= weekOfMonth + 6) {
              for (int d = 0; d < 7; ++d) {
                int date =
                  ((weekNo - 1) * 7 + d + doyOfStartOfWeek1 - doyOfMonth1) + 1;
                if (date >= 1 && date <= nDays) {
                  udates.add(date);
                }
              }
            }
          }
          dates = udates.toIntArray();
        }

        @Override
        boolean generate(DTBuilder builder) {

          // this is a bit odd, since we're generating days within the given
          // weeks of the year within the month/year from builder
          if (year != builder.year || month != builder.month) {
            if (year != builder.year) {
              year = builder.year;
              checkYear();
            }
            month = builder.month;
            checkMonth();

            i = 0;
          }

          if (i >= dates.length) { return false; }
          builder.day = dates[i++];
          return true;
        }

        @Override
        public String toString() { return "byWeekNoGenerator"; }
      };
  }

  /**
   * constructs a day generator that generates dates in the current month that
   * fall on one of the given days of the year.
   * @param yearDays elements in [-366,366] != 0
   */
  static Generator byYearDayGenerator(int[] yearDays, final DateValue dtStart) {
    final int[] uYearDays = Util.uniquify(yearDays);

    return new Generator() {
        int year = dtStart.year();
        int month = dtStart.month();
        int[] dates;
        int i = 0;

        { checkMonth(); }

        void checkMonth() {
          // now, calculate the first week of the month
          int doyOfMonth1 = TimeUtils.dayOfYear(year, month, 1);
          int nDays = TimeUtils.monthLength(year, month);
          int nYearDays = TimeUtils.yearLength(year);
          IntSet udates = new IntSet();
          for (int j = 0; j < uYearDays.length; j++) {
            int yearDay = uYearDays[j];
            if (yearDay < 0) { yearDay += nYearDays + 1; }
            int date = yearDay - doyOfMonth1;
            if (date >= 1 && date <= nDays) { udates.add(date); }
          }
          dates = udates.toIntArray();
        }

        @Override
        boolean generate(DTBuilder builder) {
          if (year != builder.year || month != builder.month) {
            year = builder.year;
            month = builder.month;

            checkMonth();

            i = 0;
          }
          if (i >= dates.length) { return false; }
          builder.day = dates[i++];
          return true;
        }

        @Override
        public String toString() { return "byYearDayGenerator"; }
      };
  }

  private Generators() {
    // uninstantiable
  }

}
