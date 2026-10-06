package yoshi

import yoshi.defaults.*
import zio.test.*

object ValidationChainingSpec extends ZIOSpecDefault {

  case class Member(name: String, age: Int)

  given Validation[Violation, (String, Int), Member] =
    Validation
      .ensureOr[Violation, (String, Int)] { case (_, age) => Violation.TooSmall(age, 18) } { case (_, age) => age >= 18 }
      .map(Member.apply.tupled)

  override def spec = suiteAll("Chaining on an already validated Either") {
    suiteAll("andValidateAs") {
      test("feeds the value into the next validation") {
        for {
          result <- Some("42").validateAs[String].andValidateAs[Int]
        } yield {
          assertTrue(result == 42)
        }
      }
      test("accepts a result typed as a Right or a Left") {
        assertTrue(
          Right("42").andValidateAs[Int] == Right(42),
          Left(Violations.of(Violation.Required)).andValidateAs[Int] == Left(Violations.of(Violation.Required)),
          Right(42).validateN(_ + 1) == Right(43),
          Right(15).validateWith(age => Left(Violations.of(Violation.TooSmall(age, 18)))) ==
            Left(Violations.of(Violation.TooSmall(15, 18))),
        )
      }
      test("accepts tuple elements typed as a Right or a Left") {
        assertTrue(
          (Right("Bob"), Right(42)).andValidateAs[Member] == Right(Member("Bob", 42)),
          (Left(Violations.of(Violation.Required)), Right(42)).validateN(identity) == Left(Violations.of(Violation.Required)),
        )
      }
      test("keeps the first violations without running the next validation") {
        assertTrue(
          Option.empty[String].validateAs[String].andValidateAs[Int].is(_.left) ==
            Violations.of(Violation.Required),
        )
      }
      test("reports the violations of the next validation") {
        assertTrue(
          Some("abc").validateAs[String].andValidateAs[Int].is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")),
        )
      }
      test("puts both steps under the path attached after the chain") {
        assertTrue(
          Option.empty[String].validateAs[String].andValidateAs[Int].at("age").is(_.left) ==
            Violations.of(Violation.Required).asChild("age"),
          Some("abc").validateAs[String].andValidateAs[Int].at("age").is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")).asChild("age"),
        )
      }
      test("feeds the combined value of a tuple into the next validation") {
        for {
          result <- (
            Some("Bob").validateAs[String].at("name"),
            Some("42").validateAs[Int].at("age"),
          ).andValidateAs[Member]
        } yield {
          assertTrue(result == Member("Bob", 42))
        }
      }
      test("accumulates the violations of every tuple element without running the next validation") {
        assertTrue(
          (
            Option.empty[String].validateAs[String].at("name"),
            Some("abc").validateAs[Int].at("age"),
          ).andValidateAs[Member].is(_.left) ==
            Violations.of(Violation.Required).asChild("name") ++
            Violations.of(Violation.NonIntegerString("abc")).asChild("age"),
        )
      }
      test("reports the violations of the next validation on the combined tuple value") {
        assertTrue(
          (
            Some("Bob").validateAs[String].at("name"),
            Some("15").validateAs[Int].at("age"),
          ).andValidateAs[Member].is(_.left) ==
            Violations.of(Violation.TooSmall(15, 18)),
        )
      }
      test("leaves a tuple of unvalidated values to validateAs") {
        assertTrue(("Bob", 42).validateAs[Member] == Right(Member("Bob", 42)))
      }
    }
    suiteAll("validateWith") {
      test("applies a function that validates a single validated value") {
        assertTrue(
          Some("15")
            .validateAs[Int]
            .validateWith { age =>
              Left(Violations.of(Violation.TooSmall(age, 18)))
            }
            .is(_.left) ==
            Violations.of(Violation.TooSmall(15, 18)),
        )
      }
      test("applies a function that validates the combined value") {
        for {
          result <- (
            Some("Bob").validateAs[String].at("name"),
            Some("42").validateAs[Int].at("age"),
          ).validateWith { case (name, age) =>
            Right(Member(name, age))
          }
        } yield {
          assertTrue(result == Member("Bob", 42))
        }
      }
      test("accumulates the violations of every element before applying the function") {
        assertTrue(
          (
            Option.empty[String].validateAs[String].at("name"),
            Some("abc").validateAs[Int].at("age"),
          ).validateWith { case (name, age) =>
            Right(Member(name, age))
          }.is(_.left) ==
            Violations.of(Violation.Required).asChild("name") ++
            Violations.of(Violation.NonIntegerString("abc")).asChild("age"),
        )
      }
      test("reports the violations the function itself produces") {
        assertTrue(
          (
            Some("Bob").validateAs[String].at("name"),
            Some("15").validateAs[Int].at("age"),
          ).validateWith { case (_, age) =>
            Left(Violations.of(Violation.TooSmall(age, 18)).asChild("age"))
          }.is(_.left) ==
            Violations.of(Violation.TooSmall(15, 18)).asChild("age"),
        )
      }
    }
  }
}
