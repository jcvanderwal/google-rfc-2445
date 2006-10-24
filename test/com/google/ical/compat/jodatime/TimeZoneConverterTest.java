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

import com.google.ical.util.TimeUtils;
import com.google.ical.values.DateValue;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TimeZone;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 *
 * @author mikesamuel+svn@gmail.com (Mike Samuel)
 */
public class TimeZoneConverterTest extends TestCase {

  long seed = System.currentTimeMillis();
  Random rand;

  @Override
  protected void setUp() throws Exception {
    System.out.println("RANDOM SEED " + seed + " : " + getName());
    rand = new Random(seed);
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testConvertMonteCarloToAndFromUtc() throws Exception {
    TimeZone utilTz = TimeZone.getTimeZone("America/Los_Angeles");
    TimeZone jodaTz = TimeZoneConverter.toTimeZone(
        DateTimeZone.forID("America/Los_Angeles"));
    for (int run = 5000; --run >= 0;) {
      long t = rand.nextInt(366 * 24 * 60 * 60 * 1000);
      DateTime randDate = new DateTime(t);
      DateValue dtv = DateTimeIteratorFactory.dateTimeToDateValue(randDate);
      assertEquals(
          TimeUtils.fromUtc(dtv, utilTz),
          TimeUtils.fromUtc(dtv, jodaTz));
      assertEquals(
          TimeUtils.toUtc(dtv, utilTz),
          TimeUtils.toUtc(dtv, jodaTz));
    }
  }

  private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
  private static final long MILLIS_PER_YEAR = (long) (365.25 * MILLIS_PER_DAY);

  public void testOffsetMonteCarlo() throws Exception {
    String[] tzids = {
      "America/Los_Angeles",  // one in the Western hemisphire with daylight
      "UTC",  // UTC
      "Europe/Paris",  // one in the Eastern hemisphere with daylight savings
      "Asia/Shanghai",  // one without daylight savings
      "Pacific/Tongatapu",  // outside [-12,+12]
    };

    long soon = System.currentTimeMillis() + (7 * MILLIS_PER_DAY);
    for (String tzid : tzids) {
      TimeZone utilTz = TimeZone.getTimeZone(tzid);
      DateTimeZone jodaTz = DateTimeZone.forID(tzid);
      // make sure that the timezone is recognized and we're not just testing
      // UTC over and over.
      assertTrue(utilTz.getID(),
                 "UTC".equals(utilTz.getID()) ||
                 !utilTz.hasSameRules(TimeZone.getTimeZone("UTC")));

      // check that we're working a week out.
      assertOffsetsEqualForDate(utilTz, jodaTz, soon);

      // generate a bunch of random times in 2006 and test that the offsets
      // convert properly
      for (int run = 5000; --run >= 0;) {
        // pick a random time in 2006
        long t = ((2000 - 1970) * MILLIS_PER_YEAR)
                 + (rand.nextLong() % (10L * MILLIS_PER_YEAR));
        assertOffsetsEqualForDate(utilTz, jodaTz, t);
      }
    }
  }

  private static void assertOffsetsEqualForDate(
      TimeZone utilTz, DateTimeZone jodaTz, long offset) {

    TimeZone convertedTz = TimeZoneConverter.toTimeZone(jodaTz);

    // Test that the util timezone and it's joda timezone are equivalent.
    assertEquals("offset=" + offset + " in " + utilTz.getID(),
                 utilTz.getOffset(offset), convertedTz.getOffset(offset));

    // Test the complicated getOffset method.
    // We don't care which tz the output fields are in since we're not
    // concerned that the output from getOffset(...) == offset,
    // just that the utilTz.getOffset(...) == jodaTz.getOffset(...) computed
    // from it are equal for both timezones.
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(new Date(offset));
    assertEquals("offset=" + offset + " in " + utilTz.getID(),
                 utilTz.getOffset(
                     c.get(Calendar.ERA),
                     c.get(Calendar.YEAR),
                     c.get(Calendar.MONTH),
                     c.get(Calendar.DAY_OF_MONTH),
                     c.get(Calendar.DAY_OF_WEEK),
                     (int) (offset % MILLIS_PER_DAY)),
                 convertedTz.getOffset(
                     c.get(Calendar.ERA),
                     c.get(Calendar.YEAR),
                     c.get(Calendar.MONTH),
                     c.get(Calendar.DAY_OF_MONTH),
                     c.get(Calendar.DAY_OF_WEEK),
                     (int) (offset % MILLIS_PER_DAY)));
  }
}
