package yoshi.interop

import _root_.cats.kernel.Monoid
import yoshi.Violations

private[interop] trait CatsViolationsInstances {

  /** Merges violation trees, which is what every accumulating cats combinator needs from an error type.
    *
    * With this instance `Validated[Violations[V], *]` accumulates through `mapN`, and `Either[Violations[V], *]` accumulates through
    * `parMapN`. `Semigroup[Violations[V]]` resolves through it as well.
    */
  implicit def violationsMonoid[V]: Monoid[Violations[V]] = new Monoid[Violations[V]] {
    def empty: Violations[V]                                       = Violations.empty
    def combine(x: Violations[V], y: Violations[V]): Violations[V] = x ++ y
  }
}
