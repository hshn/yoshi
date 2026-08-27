package yoshi

import scala.annotation.implicitNotFound

/** Supplies the violation to report when a required value is absent.
  *
  * [[Validation]] is parameterized over the violation type, so the instances derived for `Option` cannot produce a violation on their own.
  * A single `Required` instance for your violation type unlocks every `Option[A]` to `B` derivation.
  *
  * {{{
  * enum MyViolation:
  *   case Missing
  *   case NotAnInt(value: String)
  *
  * given Required[MyViolation] = Required(MyViolation.Missing)
  * given Validation[MyViolation, String, Int] = Validation.parseInt(MyViolation.NotAnInt(_))
  *
  * // Option[String] => Int is now available: None fails with Missing,
  * // Some("x") fails with NotAnInt("x")
  * val v = summon[Validation[MyViolation, Option[String], Int]]
  * }}}
  *
  * `yoshi.defaults` provides an instance for its own violation type, so importing `yoshi.defaults.*` is enough to get these derivations.
  *
  * @tparam V
  *   violation type
  */
@implicitNotFound(
  "No given Required[${V}] was found, so the violation for an absent value is undefined. " +
    "Define `given Required[${V}]`, or import yoshi.defaults.* to use the built-in violation type.",
)
trait Required[V] {

  /** The violation reported when the value is absent. */
  def violation: V
}

object Required {

  /** Create a [[Required]] from the violation to report for an absent value. */
  def apply[V](v: => V): Required[V] = new Required[V] {
    def violation: V = v
  }
}
