package com.google.ical.values;

import java.text.ParseException;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class RDataConverter {
    private RDataConverter() {
    }

    private static final Pattern FOLD = Pattern.compile("(?:\\r\\n?|\\n)[ \t]");
    private static final Pattern NEWLINE = Pattern.compile("[\\r\\n]+");
    private static final Pattern RULE = Pattern.compile("^(?:R|EX)RULE[:;]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE = Pattern.compile("^(?:R|EX)DATE[:;]", Pattern.CASE_INSENSITIVE);
    private static final Logger LOGGER = Logger.getLogger(RDataConverter.class.getName());

    public static Recurrence convert(String rdata, TimeZone tzid, boolean strict) throws ParseException {
        Recurrence result = new Recurrence();

        String unfolded = FOLD.matcher(rdata).replaceAll("").trim();
        if ("".equals(unfolded)) {
            return result;
        }
        String[] lines = NEWLINE.split(unfolded);
        
        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i].trim();
            try {
                if (RULE.matcher(line).find()) {
                    RRule rule = new RRule(line);
                    if ("rrule".equalsIgnoreCase(rule.getName()))
                    {
                        result.addInclusionRule(rule);
                    }
                    else
                    {
                        result.addExclusionRule(rule);
                    }
                } else if (DATE.matcher(line).find()) {
                    RDateList dateList = new RDateList(line, tzid);
                    if ("rdate".equalsIgnoreCase(dateList.getName()))
                    {
                        result.addInclusionDateList(dateList);
                    }
                    else
                    {
                        result.addExclusionDateList(dateList);
                    }
                } else {
                    throw new ParseException(lines[i], i);
                }
            } catch (ParseException ex) {
                if (strict) {
                    throw ex;
                }
                LOGGER.log(Level.SEVERE, "Dropping bad recurrence rule line: " + line, ex);
            } catch (IllegalArgumentException ex) {
                if (strict) {
                    throw ex;
                }
                LOGGER.log(Level.SEVERE, "Dropping bad recurrence rule line: " + line, ex);
            }
        }

        return result;
    }
}
