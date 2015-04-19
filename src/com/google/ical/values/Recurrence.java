package com.google.ical.values;

import java.util.ArrayList;
import java.util.List;

/**
 * Recurrence Component Container
 * 
 * @author bwarminski
 */
public class Recurrence {
    private List<RRule> inclusionRules = new ArrayList<>();
    private List<RDateList> inclusionDates = new ArrayList<>();
    private List<RRule> exclusionRules = new ArrayList<>();
    private List<RDateList> exclusionDates = new ArrayList<>();

    public List<RRule> getInclusionRules() {
        return inclusionRules;
    }

    public void addInclusionRule(RRule rule) {
        inclusionRules.add(checkType(checkArgNotNull(rule), "rrule"));
    }

    public List<RDateList> getInclusionDates() {
        return inclusionDates;
    }

    public void addInclusionDateList(RDateList list) {
        inclusionDates.add(checkType(checkArgNotNull(list), "rdate"));
    }

    public List<RRule> getExclusionRules() {
        return exclusionRules;
    }

    public void addExclusionRule(RRule rule) {
        exclusionRules.add(checkType(checkArgNotNull(rule), "exrule"));
    }

    public List<RDateList> getExclusionDates() {
        return exclusionDates;
    }

    public void addExclusionDateList(RDateList list) {
        exclusionDates.add(checkType(checkArgNotNull(list), "exdate"));
    }

    private static <T> T checkArgNotNull(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Expecting non-null object");
        }

        return value;
    }

    private static <T extends IcalObject> T checkType(T value, String expectedType) {
        if (!expectedType.equalsIgnoreCase(value.getName())) {
            throw new IllegalArgumentException("Expecting IcalObject named " + expectedType);
        }

        return value;
    }
}
