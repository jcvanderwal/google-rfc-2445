// CopyrightGoogle Inc. All rights reserved.

package com.google.ical.util;

import java.io.Serializable;

/**
 * static methods for creating the standard set of {@link Predicate} objects.
 */
public class Predicates {

  private Predicates() { }

  /*
   * For constant Predicates a single instance will suffice; we'll cast it to
   * the right parameterized type on demand.
   */

  private static final Predicate<?> ALWAYS_TRUE = new AlwaysTruePredicate();
  private static final Predicate<?> ALWAYS_FALSE = new AlwaysFalsePredicate();

  /**
   * Returns a Predicate that always evaluates to true.
   */
  @SuppressWarnings("unchecked")
  public static <T> Predicate<T> alwaysTrue() {
    return (Predicate<T>) ALWAYS_TRUE;
  }

  /**
   * Returns a Predicate that always evaluates to false.
   */
  @SuppressWarnings("unchecked")
  public static <T> Predicate<T> alwaysFalse() {
    return (Predicate<T>) ALWAYS_FALSE;
  }

  /**
   * Returns a Predicate that evaluates to true iff the given Predicate
   * evaluates to false.
   */
  public static <T> Predicate<T> not(Predicate<? super T> predicate) {
    assert null != predicate;
    return new NotPredicate<T>(predicate);
  }

  /**
   * Returns a Predicate that evaluates to true iff each of its components
   * evaluates to true.  The components are evaluated in order, and evaluation
   * will be "short-circuited" as soon as the answer is determined.  Does not
   * defensively copy the array passed in, so future changes to it will alter
   * the behavior of this Predicate.
   */
  public static <T> Predicate<T> and(Predicate<? super T>... components) {
    assert null != components;
    return new AndPredicate<T>(components);
  }

  /**
   * Returns a Predicate that evaluates to true iff any one of its components
   * evaluates to true.  The components are evaluated in order, and evaluation
   * will be "short-circuited" as soon as the answer is determined.  Does not
   * defensively copy the array passed in, so future changes to it will alter
   * the behavior of this Predicate.
   */
  public static <T> Predicate<T> or(Predicate<? super T>... components) {
    assert components != null;
    return new OrPredicate<T>(components);
  }

  /** @see Predicates#alwaysTrue */
  private static class AlwaysTruePredicate<T> implements Predicate<T>,
      Serializable {
    private static final long serialVersionUID = 8759914710239461322L;
    public boolean apply(T t) {
      return true;
    }
  }

  /** @see Predicates#alwaysFalse */
  private static class AlwaysFalsePredicate<T> implements Predicate<T>,
      Serializable {
    private static final long serialVersionUID = -565481022115659695L;
    public boolean apply(T t) {
      return false;
    }
  }

  /** @see Predicates#not */
  private static class NotPredicate<T> implements Predicate<T>, Serializable {
    private static final long serialVersionUID = -5113445916422049953L;
    private final Predicate<? super T> predicate;

    private NotPredicate(Predicate<? super T> predicate) {
      this.predicate = predicate;
    }
    public boolean apply(T t) {
      return !predicate.apply(t);
    }
  }

  /** @see Predicates#and */
  private static class AndPredicate<T> implements Predicate<T>, Serializable {
    private static final long serialVersionUID = 1022358602593297546L;
    private final Predicate<? super T>[] components;

    private AndPredicate(Predicate<? super T>... components) {
      this.components = components;
    }
    public boolean apply(T t) {
      for (Predicate<? super T> predicate : components) {
        if (!predicate.apply(t)) {
          return false;
        }
      }
      return true;
    }
  }

  /** @see Predicates#or */
  private static class OrPredicate<T> implements Predicate<T>, Serializable {
    private static final long serialVersionUID = -7942366790698074803L;
    private final Predicate<? super T>[] components;

    private OrPredicate(Predicate<? super T>... components) {
      this.components = components;
    }
    public boolean apply(T t) {
      for (Predicate<? super T> predicate : components) {
        if (predicate.apply(t)) {
          return true;
       }
     }
       return false;
    }
  }

}
