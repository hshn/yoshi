package yoshi

import yoshi.defaults.*
import zio.test.*

object ValidationChainingSpec extends ZIOSpecDefault {

  case class Member(name: String, age: Int)

  override def spec = suiteAll("Chaining on an already validated Either") {
    suiteAll("andValidateAs") {
      test("feeds the value into the next validation") {
        for {
          result <- Some("42").validateAs[String].andValidateAs[Int]
        } yield {
          assertTrue(result == 42)
        }
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
    }
    suiteAll("validateWith") {
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
