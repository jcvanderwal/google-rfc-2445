#Google RFC 2445

This is a mavenized version of the google-rfc-2455 project that lives on
https://code.google.com/p/google-rfc-2445/

##Purpose

RFC 2445 describes a scheme for calendar interoperability.
This project implements core parts of RFC 2445 including a parser for recurrence rules, and date lists, and a mechanism for evaluating recurrence rules.

Features:
* evaluates recurrence rules that don't occur more frequently than daily
* evaluates groups of recurrence rules and handles exceptions
* Support for Joda-time dates and java.util.Date

To-Do:
* Direct support for recurrences more frequent than daily
* Support for user-defined timezones

Requirements:
* JDK 1.5

Maturity:
Stable -- deployed in a large scale calendaring application
Efficient for all common recurrences and reasonably efficient for others (Run tests for micro-benchmarks)
No known non-halting behaviors

##Support

For questions and the occasional answer, join the user & developer group.

## Online Documentation

The javadoc is available [online](http://google-rfc-2445.googlecode.com/svn/trunk/snapshot/docs/index.html).


## Building

Prerequisites:

* Java JDK 7 or higher
* [maven 3](http://maven.apache.org)

To build the jar:

    git clone https://github.com/jcvanderwal/google-rfc-2445.git
    git checkout mavenized
    mvn clean install

## Using

Using the API is pretty easy.   Pass in some ical and you get back
a date iterable, which can be used in a for-each loop thusly:

```java
// A compatibility layer for joda-time
import com.google.ical.compat.jodatime.LocalDateIteratorFactory;
// A Joda time class that represents a day regardless of timezone
import org.joda.time.LocalDate;

public class ThirteenFridaysTheThirteenth {

  /** print the first 13 Friday the 13ths in the 3rd millenium AD. */
  public static void main(String[] args) throws java.text.ParseException {
    LocalDate start = new LocalDate(2001, 4, 13);

    // Every friday the thirteenth.
    String ical = "RRULE:FREQ=MONTHLY"
                  + ";BYDAY=FR"  // every Friday
                  + ";BYMONTHDAY=13"  // that occurs on the 13th of the month
                  + ";COUNT=13";  // stop after 13 occurences

    // Print out each date in the series.
    for (LocalDate date :
         LocalDateIteratorFactory.createLocalDateIterable(ical, start, true)) {
      System.out.println(date);
    }
  }
}
```

See [RFC 2445](rfc2445.html#4.3.10) for the recurrence rule
syntax and what it means, and the examples [later](rfc2445.html#4.8.5.4) in the same document.

If you use `java.util.Date` and `java.util.Calendar` in
your application instead of Joda-Time, you can use the
`com.google.ical.compat.javautil` package instead to provide
Date objects.
