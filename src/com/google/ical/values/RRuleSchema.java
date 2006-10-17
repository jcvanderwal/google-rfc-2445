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

package com.google.ical.values;

import com.google.ical.util.TimeUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ical schema for parsing RRULE and EXRULE content lines.
 *
 * @author mikesamuel+svn@gmail.com (Mike Samuel)
 */
class RRuleSchema extends IcalSchema{

  private static final Pattern COMMA = Pattern.compile(",");
  private static final Pattern SEMI = Pattern.compile(";");
  private static final Pattern X_NAME_RE = Pattern.compile(
      "^X-", Pattern.CASE_INSENSITIVE);
  private static final Pattern RRULE_PARTS = Pattern.compile(
      "^(FREQ|UNTIL|COUNT|INTERVAL|BYSECOND|BYMINUTE|BYHOUR|BYDAY|BYMONTHDAY|"
      + "BYYEARDAY|BYWEEKDAY|BYWEEKNO|BYMONTH|BYSETPOS|WKST|X-[A-Z0-9\\-]+)="
      + "(.*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern NUM_DAY = Pattern.compile(
      "^([+\\-]?\\d\\d?)?(SU|MO|TU|WE|TH|FR|SA)$", Pattern.CASE_INSENSITIVE);
  /////////////////////////////////
  // ICAL Object Schema
  /////////////////////////////////

  private static RRuleSchema instance;
  static RRuleSchema instance() {
    if (null == instance) {
      instance = new RRuleSchema();
    }
    return instance;
  }

  private RRuleSchema() {
    super(PARAM_RULES, CONTENT_RULES, OBJECT_RULES, XFORM_RULES);
  }

  private static Map<String, ParamRule> PARAM_RULES =
    new HashMap<String, ParamRule>();

  private static Map<String, ContentRule> CONTENT_RULES =
    new HashMap<String, ContentRule>();

  private static Map<String, ObjectRule> OBJECT_RULES =
    new HashMap<String, ObjectRule>();

  private static Map<String, XformRule> XFORM_RULES =
    new HashMap<String, XformRule>();

  static {
    // rrule      = "RRULE" rrulparam ":" recur CRLF
    // exrule     = "EXRULE" exrparam ":" recur CRLF
    OBJECT_RULES.put("RRULE", new ObjectRule() {
        public void apply(
            IcalSchema schema, Map<String, String> params, String content,
            IcalObject target)
            throws ParseException {
          schema.applyParamsSchema("rrulparam", params, target);
          schema.applyContentSchema("recur", content, target);
        }
      });
    OBJECT_RULES.put("EXRULE", new ObjectRule() {
        public void apply(
            IcalSchema schema, Map<String, String> params, String content,
            IcalObject target)
            throws ParseException {
          schema.applyParamsSchema("exrparam", params, target);
          schema.applyContentSchema("recur", content, target);
        }
      });

    // rrulparam  = *(";" xparam)
    // exrparam   = *(";" xparam)
    ParamRule xparamsOnly = new ParamRule() {
        public void apply(
            IcalSchema schema, String name, String value, IcalObject out)
            throws ParseException {
          schema.badParam(name, value);
        }
      };
    PARAM_RULES.put("rrulparam", xparamsOnly);
    PARAM_RULES.put("exrparam", xparamsOnly);

    /*
     * recur      = "FREQ"=freq *(
     *
     *            ; either UNTIL or COUNT may appear in a 'recur',
     *            ; but UNTIL and COUNT MUST NOT occur in the same 'recur'
     *
     *            ( ";" "UNTIL" "=" enddate ) /
     *            ( ";" "COUNT" "=" 1*DIGIT ) /
     *
     *            ; the rest of these keywords are optional,
     *            ; but MUST NOT occur more than once
     *
     *            ( ";" "INTERVAL" "=" 1*DIGIT )          /
     *            ( ";" "BYSECOND" "=" byseclist )        /
     *            ( ";" "BYMINUTE" "=" byminlist )        /
     *            ( ";" "BYHOUR" "=" byhrlist )           /
     *            ( ";" "BYDAY" "=" bywdaylist )          /
     *            ( ";" "BYMONTHDAY" "=" bymodaylist )    /
     *            ( ";" "BYYEARDAY" "=" byyrdaylist )     /
     *            ( ";" "BYWEEKNO" "=" bywknolist )       /
     *            ( ";" "BYMONTH" "=" bymolist )          /
     *            ( ";" "BYSETPOS" "=" bysplist )         /
     *            ( ";" "WKST" "=" weekday )              /
     *
     *            ( ";" x-name "=" text )
     *            )
     */
    CONTENT_RULES.put("recur", new ContentRule() {
        public void apply(IcalSchema schema, String content, IcalObject target)
            throws ParseException {
          String[] parts = SEMI.split(content);
          Map<String, String> partMap = new HashMap<String, String>();
          for (int i = 0; i < parts.length; ++i) {
            String p = parts[i];
            Matcher m = RRULE_PARTS.matcher(p);
            if (!m.matches()) { schema.badPart(p, null); }
            String k = m.group(1).toUpperCase(),
                   v = m.group(2);
            if (partMap.containsKey(k)) { schema.dupePart(p); }
            partMap.put(k, v);
          }
          if (!partMap.containsKey("FREQ")) {
            schema.missingPart("FREQ", content);
          }
          if (partMap.containsKey("UNTIL") && partMap.containsKey("COUNT")) {
            schema.badPart(content, "UNTIL & COUNT are exclusive");
          }
          for (Map.Entry<String, String> part : partMap.entrySet()) {
            if (X_NAME_RE.matcher(part.getKey()).matches()) {
              // ignore x-name content parts
              continue;
            }
            schema.applyContentSchema(part.getKey(), part.getValue(), target);
          }
        }
      });

    // exdate     = "EXDATE" exdtparam ":" exdtval *("," exdtval) CRLF
    OBJECT_RULES.put("EXDATE", new ObjectRule() {
        public void apply(
            IcalSchema schema, Map<String, String> params, String content,
            IcalObject target)
            throws ParseException {
          schema.applyParamsSchema("exdtparam", params, target);
          for (String part : COMMA.split(content)) {
            schema.applyContentSchema("exdtval", part, target);
          }
        }
      });

    // "FREQ"=freq *(
    CONTENT_RULES.put("FREQ", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setFreq(
              (Frequency) schema.applyXformSchema("freq", value));
        }
      });

    //  ( ";" "UNTIL" "=" enddate ) /
    CONTENT_RULES.put("UNTIL", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setUntil(
              (DateValue) schema.applyXformSchema("enddate", value));
        }
      });

    //  ( ";" "COUNT" "=" 1*DIGIT ) /
    CONTENT_RULES.put("COUNT", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setCount(Integer.parseInt(value));
        }
      });

    //  ( ";" "INTERVAL" "=" 1*DIGIT )          /
    CONTENT_RULES.put("INTERVAL", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setInterval(Integer.parseInt(value));
        }
      });

    //  ( ";" "BYSECOND" "=" byseclist )        /
    CONTENT_RULES.put("BYSECOND", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setBySecond(
              (int[]) schema.applyXformSchema("byseclist", value));
        }
      });

    //  ( ";" "BYMINUTE" "=" byminlist )        /
    CONTENT_RULES.put("BYMINUTE", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setByMinute(
              (int[]) schema.applyXformSchema("byminlist", value));
        }
      });

    //  ( ";" "BYHOUR" "=" byhrlist )           /
    CONTENT_RULES.put("BYHOUR", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setByHour(
              (int[]) schema.applyXformSchema("byhrlist", value));
        }
      });

    //  ( ";" "BYDAY" "=" bywdaylist )          /
    CONTENT_RULES.put("BYDAY", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setByDay(
              (List<WeekdayNum>) schema.applyXformSchema("bywdaylist", value));
        }
      });

    //  ( ";" "BYMONTHDAY" "=" bymodaylist )    /
    CONTENT_RULES.put("BYMONTHDAY", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setByMonthDay(
              (int[]) schema.applyXformSchema("bymodaylist", value));
        }
      });

    //  ( ";" "BYYEARDAY" "=" byyrdaylist )     /
    CONTENT_RULES.put("BYYEARDAY", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setByYearDay(
              (int[]) schema.applyXformSchema("byyrdaylist", value));
        }
      });

    //  ( ";" "BYWEEKNO" "=" bywknolist )       /
    CONTENT_RULES.put("BYWEEKNO", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setByWeekNo(
              (int[]) schema.applyXformSchema("bywknolist", value));
        }
      });

    //  ( ";" "BYMONTH" "=" bymolist )          /
    CONTENT_RULES.put("BYMONTH", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setByMonth(
              (int[]) schema.applyXformSchema("bymolist", value));
        }
      });

    //  ( ";" "BYSETPOS" "=" bysplist )         /
    CONTENT_RULES.put("BYSETPOS", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setBySetPos(
              (int[]) schema.applyXformSchema("bysplist", value));
        }
      });

    //  ( ";" "WKST" "=" weekday )              /
    CONTENT_RULES.put("WKST", new ContentRule() {
        public void apply(IcalSchema schema, String value, IcalObject target)
            throws ParseException {
          ((RRule) target).setWkSt(
              (Weekday) schema.applyXformSchema("weekday", value));
        }
      });

    // freq       = "SECONDLY" / "MINUTELY" / "HOURLY" / "DAILY"
    //            / "WEEKLY" / "MONTHLY" / "YEARLY"
    XFORM_RULES.put("freq", new XformRule() {
        public Frequency apply(IcalSchema schema, String value)
            throws ParseException {
          return Frequency.valueOf(value);
        }
      });

    // enddate    = date
    // enddate    =/ date-time            ;An UTC value
    XFORM_RULES.put("enddate", new XformRule() {
        public DateValue apply(IcalSchema schema, String value)
            throws ParseException {
          return IcalParseUtil.parseDateValue(value.toUpperCase());
        }
      });

    // byseclist  = seconds / ( seconds *("," seconds) )
    // seconds    = 1DIGIT / 2DIGIT       ;0 to 59
    XFORM_RULES.put("byseclist", new XformRule() {
        public int[] apply(IcalSchema schema, String value)
            throws ParseException {
          return parseUnsignedIntList(value, 0, 59, schema);
        }
      });

    // byminlist  = minutes / ( minutes *("," minutes) )
    // minutes    = 1DIGIT / 2DIGIT       ;0 to 59
    XFORM_RULES.put("byminlist", new XformRule() {
        public int[] apply(IcalSchema schema, String value)
            throws ParseException {
          return parseUnsignedIntList(value, 0, 59, schema);
        }
      });

    // byhrlist   = hour / ( hour *("," hour) )
    // hour       = 1DIGIT / 2DIGIT       ;0 to 23
    XFORM_RULES.put("byhrlist", new XformRule() {
        public int[] apply(IcalSchema schema, String value)
            throws ParseException {
          return parseUnsignedIntList(value, 0, 23, schema);
        }
      });

    // plus       = "+"
    // minus      = "-"

    // bywdaylist = weekdaynum / ( weekdaynum *("," weekdaynum) )
    // weekdaynum = [([plus] ordwk / minus ordwk)] weekday
    // ordwk      = 1DIGIT / 2DIGIT       ;1 to 53
    // weekday    = "SU" / "MO" / "TU" / "WE" / "TH" / "FR" / "SA"
    // ;Corresponding to SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY,
    // ;FRIDAY, SATURDAY and SUNDAY days of the week.
    XFORM_RULES.put("bywdaylist", new XformRule() {
        public List<WeekdayNum> apply(IcalSchema schema, String value)
            throws ParseException {
          String[] parts = COMMA.split(value);
          List<WeekdayNum> weekdays = new ArrayList<WeekdayNum>(parts.length);
          for (String p : parts) {
            Matcher m = NUM_DAY.matcher(p);
            if (!m.matches()) { schema.badPart(p, null); }
            Weekday wday = Weekday.valueOf(m.group(2).toUpperCase());
            int n;
            String numText = m.group(1);
            if (null == numText || "".equals(numText)) {
              n = 0;
            } else {
              n = Integer.parseInt(numText);
              int absn = n < 0 ? -n : n;
              if (!(1 <= absn && 53 >= absn)) { schema.badPart(p, null); }
            }
            weekdays.add(new WeekdayNum(n, wday));
          }
          return weekdays;
        }
      });

    XFORM_RULES.put("weekday", new XformRule() {
        public Weekday apply(IcalSchema schema, String value)
            throws ParseException {
          return Weekday.valueOf(value.toUpperCase());
        }
      });

    // bymodaylist = monthdaynum / ( monthdaynum *("," monthdaynum) )
    // monthdaynum = ([plus] ordmoday) / (minus ordmoday)
    // ordmoday   = 1DIGIT / 2DIGIT       ;1 to 31
    XFORM_RULES.put("bymodaylist", new XformRule() {
        public int[] apply(IcalSchema schema, String value)
            throws ParseException {
          return parseIntList(value, 1, 31, schema);
        }
      });

    // byyrdaylist = yeardaynum / ( yeardaynum *("," yeardaynum) )
    // yeardaynum = ([plus] ordyrday) / (minus ordyrday)
    // ordyrday   = 1DIGIT / 2DIGIT / 3DIGIT      ;1 to 366
    XFORM_RULES.put("byyrdaylist", new XformRule() {
        public int[] apply(IcalSchema schema, String value)
            throws ParseException {
          return parseIntList(value, 1, 366, schema);
        }
      });

    // bywknolist = weeknum / ( weeknum *("," weeknum) )
    // weeknum    = ([plus] ordwk) / (minus ordwk)
    XFORM_RULES.put("bywknolist", new XformRule() {
        public int[] apply(IcalSchema schema, String value)
            throws ParseException {
          return parseIntList(value, 1, 53, schema);
        }
      });

    // bymolist   = monthnum / ( monthnum *("," monthnum) )
    // monthnum   = 1DIGIT / 2DIGIT       ;1 to 12
    XFORM_RULES.put("bymolist", new XformRule() {
        public int[] apply(IcalSchema schema, String value)
            throws ParseException {
          return parseIntList(value, 1, 12, schema);
        }
      });

    // bysplist   = setposday / ( setposday *("," setposday) )
    // setposday  = yeardaynum
    XFORM_RULES.put("bysplist", new XformRule() {
        public int[] apply(IcalSchema schema, String value)
            throws ParseException {
          return parseIntList(value, 1, 366, schema);
        }
      });


        // rdate      = "RDATE" rdtparam ":" rdtval *("," rdtval) CRLF
    OBJECT_RULES.put("RDATE", new ObjectRule() {
        public void apply(
            IcalSchema schema, Map<String, String> params, String content,
            IcalObject target)
            throws ParseException {
          schema.applyParamsSchema("rdtparam", params, target);
          schema.applyContentSchema("rdtval", content, target);
        }
      });
    // exdate     = "EXDATE" exdtparam ":" exdtval *("," exdtval) CRLF
    // exdtparam  = rdtparam
    // exdtval    = rdtval
    OBJECT_RULES.put("EXDATE", new ObjectRule() {
        public void apply(
            IcalSchema schema, Map<String, String> params, String content,
            IcalObject target)
            throws ParseException {
          schema.applyParamsSchema("rdtparam", params, target);
          schema.applyContentSchema("rdtval", content, target);
        }
      });

    // rdtparam   = *(

    //            ; the following are optional,
    //            ; but MUST NOT occur more than once

    //            (";" "VALUE" "=" ("DATE-TIME" / "DATE" / "PERIOD")) /
    //            (";" tzidparam) /

    //            ; the following is optional,
    //            ; and MAY occur more than once

    //            (";" xparam)

    //            )


    // tzidparam  = "TZID" "=" [tzidprefix] paramtext CRLF
    // tzidprefix = "/"
    PARAM_RULES.put(
        "rdtparam",
        new ParamRule() {
          public void apply(
              IcalSchema schema, String name, String value, IcalObject out)
              throws ParseException {
            if ("value".equalsIgnoreCase(name)) {
              if ("date-time".equalsIgnoreCase(value)
                  || "date".equalsIgnoreCase(value)
                  || "period".equalsIgnoreCase(value)) {
                ((RDateList) out).setValueType(IcalValueType.fromIcal(value));
              } else {
                schema.badParam(name, value);
              }
            } else if ("tzid".equalsIgnoreCase(name)) {
              if (value.startsWith("/")) {
                // is globally defined name.  We treat all as globally defined.
                value = value.substring(1).trim();
              }
              // TODO(msamuel): proper timezone lookup, and warn on failure
              TimeZone tz = TimeUtils.timeZoneForName(
                  value.replaceAll(" ", "_"));
              if (null == tz) { schema.badParam(name, value); }
              ((RDateList) out).setTzid(tz);
            } else {
              schema.badParam(name, value);
            }
          }
        });
    PARAM_RULES.put("rrulparam", xparamsOnly);
    PARAM_RULES.put("exrparam", xparamsOnly);

    // rdtval     = date-time / date / period ;Value MUST match value type
    CONTENT_RULES.put("rdtval", new ContentRule() {
        public void apply(IcalSchema schema, String content, IcalObject target)
            throws ParseException {
          RDateList rdates = (RDateList) target;
          String[] parts = COMMA.split(content);
          DateValue[] datesUtc = new DateValue[parts.length];
          for (int i = 0; i < parts.length; ++i) {
            String part = parts[i];
            // TODO(msamuel): figure out what to do with periods.
            datesUtc[i] = IcalParseUtil.parseDateValue(part, rdates.getTzid());
          }
          rdates.setDatesUtc(datesUtc);
        }
      });

  }


  /////////////////////////////////
  // Parser Helper functions and classes
  /////////////////////////////////

  private static int[] parseIntList(
      String commaSeparatedString, int absmin, int absmax, IcalSchema schema)
      throws ParseException {

    String[] parts = COMMA.split(commaSeparatedString);
    int[] out = new int[parts.length];
    for (int i = parts.length; --i >= 0;) {
      try {
        int n = Integer.parseInt(parts[i]);
        int absn = Math.abs(n);
        if (!(absmin <= absn && absmax >= absn)) {
          schema.badPart(commaSeparatedString, null);
        }
        out[i] = n;
      } catch (NumberFormatException ex) {
        schema.badPart(commaSeparatedString, ex.getMessage());
      }
    }
    return out;
  }

  private static int[] parseUnsignedIntList(
      String commaSeparatedString, int min, int max, IcalSchema schema)
      throws ParseException {

    String[] parts = COMMA.split(commaSeparatedString);
    int[] out = new int[parts.length];
    for (int i = parts.length; --i >= 0;) {
      try {
        int n = Integer.parseInt(parts[i]);
        if (!(min <= n && max >= n)) {
          schema.badPart(commaSeparatedString, null);
        }
        out[i] = n;
      } catch (NumberFormatException ex) {
        schema.badPart(commaSeparatedString, ex.getMessage());
      }
    }
    return out;
  }

}
