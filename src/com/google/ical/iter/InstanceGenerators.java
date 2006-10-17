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
import com.google.ical.util.Predicate;
import com.google.ical.util.TimeUtils;
import com.google.ical.values.Frequency;
import com.google.ical.values.Weekday;
import com.google.ical.values.DateValue;

import java.util.ArrayList;
import java.util.List;

/**
 * factory for generators that operate on groups of generators to generate full
 * dates.
 *
 * @author mikesamuel+svn@gmail.com (Mike Samuel)
 */
class InstanceGenerators {

  /**
   * a collector that yields each date in the period without doing any set
   * collecting.
   */
  static Generator serialInstanceGenerator(
      final Predicate<? super DateValue> filter,
      final Generator yearGenerator, final Generator monthGenerator,
      final Generator dayGenerator) {
    return new Generator() {
        @Override
        public boolean generate(DTBuilder builder)
            throws IteratorShortCircuitingException {
          // cascade through periods to compute the next date
          do {
            // until we run out of days in the current month
            while (!dayGenerator.generate(builder)) {
              // until we run out of months in the current year
              while (!monthGenerator.generate(builder)) {
                // if there are more years available fetch one
                if (!yearGenerator.generate(builder)) {
                  // otherwise the recurrence is exhausted
                  return false;
                }
              }
            }
            // apply filters to generated dates
          } while (!filter.apply(builder.toDate()));

          return true;
        }
      };
  }

  static Generator bySetPosInstanceGenerator(
      int[] setPos, final Frequency freq, final Weekday wkst,
      final Predicate<? super DateValue> filter,
      final Generator yearGenerator, final Generator monthGenerator,
      final Generator dayGenerator) {
    final int[] uSetPos = Util.uniquify(setPos);

    final Generator serialInstanceGenerator =
      serialInstanceGenerator(
          filter, yearGenerator, monthGenerator, dayGenerator);

    final boolean allPositive;
    final int maxPos;
    if (false) {
      int mp = 0;
      boolean ap = true;
      for (int i = setPos.length; --i >= 0;) {
        if (setPos[i] < 0) {
          ap = false;
          break;
        }
        mp = Math.max(setPos[i], mp);
      }
      maxPos = mp;
      allPositive = ap;
    } else {
      // TODO(msamuel): does this work?
      maxPos = uSetPos[uSetPos.length - 1];
      allPositive = uSetPos[0] > 0;
    }

    return new Generator() {
        DateValue pushback = null;
        /**
         * Is this the first instance we generate?
         * We need to know so that we don't clobber dtStart.
         */
        boolean first = true;
        /** Do we need to halt iteration once the current set has been used? */
        boolean done = false;

        /** The elements in the current set, filtered by set pos */
        List<DateValue> candidates;
        /**
         * index into candidates.  The number of elements in candidates already
         * consumed.
         */
        int i;

        @Override
        public boolean generate(DTBuilder builder)
            throws IteratorShortCircuitingException {
          while (null == candidates || i >= candidates.size()) {
            if (done) { return false; }

            // (1) Make sure that builder is appropriately initialized so that
            // we only generate instances in the next set

            DateValue d0 = null;
            if (null != pushback) {
              d0 = pushback;
              builder.year = d0.year();
              builder.month = d0.month();
              builder.day = d0.day();
              pushback = null;
            } else if (!first) {
              // we need to skip ahead to the next item since we didn't exhaust
              // the last period
              switch (freq) {
                case YEARLY:
                  if (!yearGenerator.generate(builder)) { return false; }
                  // fallthru
                case MONTHLY:
                  while (!monthGenerator.generate(builder)) {
                    if (!yearGenerator.generate(builder)) { return false; }
                  }
                  break;
                case WEEKLY:
                  // consume because just incrementing date doesn't do anything
                  DateValue nextWeek =
                    Util.nextWeekStart(builder.toDate(), wkst);
                  do {
                    if (!serialInstanceGenerator.generate(builder)) {
                      return false;
                    }
                  } while (builder.compareTo(nextWeek) < 0);
                  d0 = builder.toDate();
                  break;
                default:
                  break;
              }
            } else {
              first = false;
            }

            // (2) Build a set of the dates in the year/month/week that match
            // the other rule.
            List<DateValue> dates = new ArrayList<DateValue>();
            if (null != d0) { dates.add(d0); }

            // Optimization: if min(bySetPos) > 0 then we already have absolute
            // positions, so we don't need to generate all of the instances for
            // the period.
            // This speeds up things like the first weekday of the year:
            //     RRULE:FREQ=YEARLY;BYDAY=MO,TU,WE,TH,FR,BYSETPOS=1
            // that would otherwise generate 260+ instances per one emitted
            // TODO(msamuel): this may be premature.  If needed, We could
            // improve more generally by inferring a BYMONTH generator based on
            // distribution of set positions within the year.
            int limit = allPositive ? maxPos : Integer.MAX_VALUE;

            while (limit > dates.size()) {
              if (!serialInstanceGenerator.generate(builder)) {
                // If we can't generate any, then make sure we return false
                // once the instances we have generated are exhausted.
                // If this is returning false due to some artificial limit, such
                // as the 100 year limit in serialYearGenerator, then we exit
                // via an exception because otherwise we would pick the wrong
                // elements for some uSetPoses that contain negative elements.
                done = true;
              }
              DateValue d = builder.toDate();
              boolean contained = false;
              if (null == d0) {
                d0 = d;
                contained = true;
              } else {
                switch (freq) {
                  case WEEKLY:
                    int nb = TimeUtils.daysBetween(d, d0);
                    // Two dates (d, d0) are in the same week
                    // if there isn't a whole week in between them and the
                    // later day is later in the week than the earlier day.
                    contained =
                      nb < 7
                      && ((7 + Weekday.valueOf(d).javaDayNum
                           - wkst.javaDayNum) % 7)
                      > ((7 + Weekday.valueOf(d0).javaDayNum
                          - wkst.javaDayNum) % 7);
                    break;
                  case MONTHLY:
                    contained =
                      d0.month() == d.month() && d0.year() == d.year();
                    break;
                  case YEARLY:
                    contained = d0.year() == d.year();
                    break;
                  default:
                    break;
                }
              }
              if (contained) {
                dates.add(d);
              } else {
                // reached end of the set
                pushback = d;  // save d so we can use it later
                break;
              }
            }

            // (3) Resolve the positions to absolute positions and order them
            int[] absSetPos;
            if (allPositive) {
              absSetPos = uSetPos;
            } else {
              IntSet uAbsSetPos = new IntSet();
              for (int j = 0; j < uSetPos.length; ++j) {
                int p = uSetPos[j];
                if (p < 0) { p = dates.size() + p + 1; }
                uAbsSetPos.add(p);
              }
              absSetPos = uAbsSetPos.toIntArray();
            }

            candidates = new ArrayList<DateValue>();
            for (int j = 0; j < absSetPos.length; ++j) {
              int p = absSetPos[j] - 1;
              if (p >= 0 && p < dates.size()) {
                candidates.add(dates.get(p));
              }
            }
            i = 0;
            if (candidates.isEmpty()) {
              // none in this region, so keep looking
              candidates = null;
              continue;
            }
          }
          // (5) Emit a date.  It will be checked against the end condition and
          // dtStart elsewhere
          DateValue d = candidates.get(i++);
          builder.year = d.year();
          builder.month = d.month();
          builder.day = d.day();
          return true;
        }
      };

  }

  private InstanceGenerators() {
    // uninstantiable
  }
}
