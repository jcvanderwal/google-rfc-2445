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

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testConvertMonteCarlo() throws Exception {
    long seed = System.currentTimeMillis();
    Random rand = new Random(seed);
    System.out.println("seed=" + seed);

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
}
