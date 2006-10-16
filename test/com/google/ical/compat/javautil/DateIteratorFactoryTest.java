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

import com.google.ical.util.TimeUtils;
import com.google.ical.values.DateTimeValue;
import com.google.ical.values.DateTimeValueImpl;
import com.google.ical.values.DateValue;
import com.google.ical.values.DateValueImpl;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import junit.framework.TestCase;

/**
 * testcases for {@link DateIteratorFactory}.
 *
 * @author Mike Samuel (msamuel@google.com)
 */
public class DateIteratorFactoryTest extends TestCase {

  public void testDateValueToDate() throws Exception {
    assertEquals(createDateUtc(2006, 10, 13, 0, 0, 0).getTime(),
                 DateIteratorFactory.dateValueToDate(
                     new DateValueImpl(2006, 10, 13)).getTime());
    assertEquals(createDateUtc(2006, 10, 13, 12, 30, 1),
                 DateIteratorFactory.dateValueToDate(
                     new DateTimeValueImpl(2006, 10, 13, 12, 30, 1)));
  }

  public void testDateToDateTimeValue() throws Exception {
    assertEquals(new DateTimeValueImpl(2006, 10, 13, 0, 0, 0),
                 DateIteratorFactory.dateToDateValue(
                     createDateUtc(2006, 10, 13, 0, 0, 0), false));
    assertEquals(new DateValueImpl(2006, 10, 13),
                 DateIteratorFactory.dateToDateValue(
                     createDateUtc(2006, 10, 13, 0, 0, 0), true));
    assertEquals(new DateTimeValueImpl(2006, 10, 13, 12, 30, 1),
                 DateIteratorFactory.dateToDateValue(
                     createDateUtc(2006, 10, 13, 12, 30, 1), false));
    assertEquals(new DateTimeValueImpl(2006, 10, 13, 12, 30, 1),
                 DateIteratorFactory.dateToDateValue(
                     createDateUtc(2006, 10, 13, 12, 30, 1), true));
  }

  public void testConsistency() throws Exception {
    DateValue dv = new DateValueImpl(2006, 10, 13),
             dtv = new DateTimeValueImpl(2006, 10, 13, 12, 30, 1);
    assertEquals(dv, DateIteratorFactory.dateToDateValue(
                          DateIteratorFactory.dateValueToDate(dv), true));
    assertEquals(dtv, DateIteratorFactory.dateToDateValue(
                          DateIteratorFactory.dateValueToDate(dtv), true));
  }

  private Date createDateUtc(int ye, int mo, int da, int ho, int mi, int se) {
    Calendar c = new GregorianCalendar(TimeUtils.utcTimezone());
    c.clear();
    c.set(ye, mo - 1, da, ho, mi, se);
    return c.getTime();
  }
}
