package yoshi

import cats.data.Validated
import cats.kernel.Monoid
import cats.syntax.all.*
import yoshi.defaults.*
import yoshi.interop.cats.*
import zio.test.*

object CatsViolationsSpec extends ZIOSpecDefault {

  val monoid: Monoid[Violations[Violation]] = Monoid[Violations[Violation]]

  override def spec = suiteAll("Violations as a cats Monoid") {
    suiteAll("the instance itself") {
      test("empty leaves the other side untouched") {
        val violations = Violations.of(Violation.Required).asChild("name")
        assertTrue(
          monoid.combine(monoid.empty, violations) == violations,
          monoid.combine(violations, monoid.empty) == violations,
        )
      }
      test("combine merges the branches of both trees") {
        assertTrue(
          monoid.combine(
            Violations.of(Violation.Required).asChild("name"),
            Violations.of(Violation.NonIntegerString("abc")).asChild("age"),
          ) ==
            Violations.of(Violation.Required).asChild("name") ++
            Violations.of(Violation.NonIntegerString("abc")).asChild("age"),
        )
      }
      test("combineAll folds a whole list") {
        assertTrue(
          List(
            Violations.of(Violation.Required).asChild("name"),
            Violations.of(Violation.NonIntegerString("abc")).asChild("age"),
          ).combineAll ==
            Violations.of(Violation.Required).asChild("name") ++
            Violations.of(Violation.NonIntegerString("abc")).asChild("age"),
        )
      }
    }
    suiteAll("what the instance unlocks") {
      test("Validated accumulates through mapN") {
        val name: Validated[Violations[Violation], String] =
          Validated.invalid(Violations.of(Violation.Required).asChild("name"))
        val age: Validated[Violations[Violation], Int] =
          Validated.invalid(Violations.of(Violation.NonIntegerString("abc")).asChild("age"))

        val result = (name, age).mapN((_, _))

        assertTrue(
          result == Validated.invalid(
            Violations.of(Violation.Required).asChild("name") ++
              Violations.of(Violation.NonIntegerString("abc")).asChild("age"),
          ),
        )
      }
      test("Either accumulates through parMapN") {
        val result = (
          Option.empty[String].validateAs[String].at("name"),
          "abc".validateAs[Int].at("age"),
        ).parMapN((_, _))

        assertTrue(
          result.is(_.left) ==
            Violations.of(Violation.Required).asChild("name") ++
            Violations.of(Violation.NonIntegerString("abc")).asChild("age"),
        )
      }
    }
  }
}
