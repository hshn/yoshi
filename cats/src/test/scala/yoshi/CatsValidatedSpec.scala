package yoshi

import cats.data.Validated
import cats.syntax.all.*
import yoshi.defaults.*
import yoshi.interop.cats.*
import zio.test.*

object CatsValidatedSpec extends ZIOSpecDefault {

  override def spec = suiteAll("Validation and Validated") {
    suiteAll("runValidated") {
      test("reports a successful run as Valid") {
        assertTrue(Validations.parseInt.runValidated("42") == Validated.valid(42))
      }
      test("reports the violations as Invalid") {
        assertTrue(
          Validations.parseInt.runValidated("abc") ==
            Validated.invalid(Violations.of(Violation.NonIntegerString("abc"))),
        )
      }
      test("accumulates across validations through mapN") {
        val result = (
          Validations.parseInt.runValidated("abc").leftMap(_.asChild("age")),
          Validations.minLength(3).runValidated("ab").leftMap(_.asChild("name")),
        ).mapN { case (age, name) => (age, name) }

        assertTrue(
          result == Validated.invalid(
            Violations.of(Violation.NonIntegerString("abc")).asChild("age") ++
              Violations.of(Violation.TooShortString("ab", 3)).asChild("name"),
          ),
        )
      }
    }
    suiteAll("fromValidated") {
      val validation: Validation[Violation, String, Int] =
        Validation.fromValidated { value =>
          value.toIntOption match {
            case Some(int) => Validated.valid(int)
            case None      => Validated.invalid(Violations.of(Violation.NonIntegerString(value)))
          }
        }

      test("succeeds where the function returns Valid") {
        for {
          result <- validation.run("42")
        } yield {
          assertTrue(result == 42)
        }
      }
      test("fails with the violations the function returned as Invalid") {
        assertTrue(validation.run("abc").is(_.left) == Violations.of(Violation.NonIntegerString("abc")))
      }
      test("composes with the rest of the library") {
        val composed: Validation[Violation, Option[String], Int] =
          Validations.required[String] >> validation

        assertTrue(
          composed.run(None).is(_.left) == Violations.of(Violation.Required),
          composed.run(Some("abc")).is(_.left) == Violations.of(Violation.NonIntegerString("abc")),
        )
      }
    }
  }
}
