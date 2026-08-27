package yoshi

import yoshi.Violations.Path
import yoshi.defaults.*
import zio.test.*

object ValidationTypeclassSpec extends ZIOSpecDefault {
  override def spec = suiteAll("Validation typeclass instances") {
    suiteAll("optionCanBeValidatedAs") {
      test("derive Option validation from given") {
        given Validation[Violation, String, String] = Validations.minLength(1)
        val v                                       = summon[Validation[Violation, Option[String], Option[String]]]

        for {
          result <- v.run(Some("hello"))
        } yield {
          assertTrue(result == Some("hello"))
        }
      }
      test("pass through None") {
        given Validation[Violation, String, String] = Validations.minLength(1)
        val v                                       = summon[Validation[Violation, Option[String], Option[String]]]

        for {
          result <- v.run(None)
        } yield {
          assertTrue(result == None)
        }
      }
      test("fail when Some value is invalid") {
        given Validation[Violation, String, String] = Validations.minLength(5)
        val v                                       = summon[Validation[Violation, Option[String], Option[String]]]

        assertTrue(v.run(Some("ab")).is(_.left) == Violations.of(Violation.TooShortString("ab", 5)))
      }
    }
    suiteAll("optionCanBeValidatedAsRequired") {
      test("validate the value an Option holds") {
        val v = summon[Validation[Violation, Option[String], Int]]

        for {
          result <- v.run(Some("42"))
        } yield {
          assertTrue(result == 42)
        }
      }
      test("fail with the Required violation on None") {
        val v = summon[Validation[Violation, Option[String], Int]]

        assertTrue(v.run(None).is(_.left) == Violations.of(Violation.Required))
      }
      test("report the violation of the validation it delegates to") {
        val v = summon[Validation[Violation, Option[String], Int]]

        assertTrue(v.run(Some("abc")).is(_.left) == Violations.of(Violation.NonIntegerString("abc")))
      }
      test("keep passing None through when the output type is an Option") {
        given Validation[Violation, String, Int] = Validations.parseInt
        val v                                    = summon[Validation[Violation, Option[String], Option[Int]]]

        for {
          result <- v.run(None)
        } yield {
          assertTrue(result == None)
        }
      }
      test("require each level of a nested Option separately") {
        val v = summon[Validation[Violation, Option[Option[String]], Option[Int]]]

        for {
          absent  <- v.run(None)
          present <- v.run(Some(Some("42")))
        } yield {
          assertTrue(absent == None) &&
          assertTrue(present == Some(42)) &&
          assertTrue(v.run(Some(None)).is(_.left) == Violations.of(Violation.Required))
        }
      }
    }
    suiteAll("requiredCanBeValidated") {
      test("extract the value an Option holds") {
        val v = summon[Validation[Violation, Option[String], String]]

        for {
          result <- v.run(Some("hello"))
        } yield {
          assertTrue(result == "hello")
        }
      }
      test("fail with the Required violation on None") {
        val v = summon[Validation[Violation, Option[String], String]]

        assertTrue(v.run(None).is(_.left) == Violations.of(Violation.Required))
      }
    }
    suiteAll("seqCanBeValidatedAs") {
      test("validate all elements in Seq") {
        given Validation[Violation, String, String] = Validations.minLength(1)
        val v                                       = summon[Validation[Violation, Seq[String], Seq[String]]]

        for {
          result <- v.run(Seq("a", "bb", "ccc"))
        } yield {
          assertTrue(result == Seq("a", "bb", "ccc"))
        }
      }
      test("succeed with empty Seq") {
        given Validation[Violation, String, String] = Validations.minLength(1)
        val v                                       = summon[Validation[Violation, Seq[String], Seq[String]]]

        for {
          result <- v.run(Seq.empty)
        } yield {
          assertTrue(result == Seq.empty)
        }
      }
      test("accumulate violations with indices") {
        given Validation[Violation, String, String] = Validations.minLength(3)
        val v                                       = summon[Validation[Violation, Seq[String], Seq[String]]]

        val expectedViolations = Violations[Violation](
          children = Map(
            Path(0) -> Violations(Vector(Violation.TooShortString("ab", 3))),
            Path(2) -> Violations(Vector(Violation.TooShortString("x", 3))),
          ),
        )

        assertTrue(v.run(Seq("ab", "hello", "x")).is(_.left) == expectedViolations)
      }
    }
    suiteAll("listCanBeValidatedAs") {
      test("validate all elements in List") {
        given Validation[Violation, String, String] = Validations.minLength(1)
        val v                                       = summon[Validation[Violation, List[String], List[String]]]

        for {
          result <- v.run(List("a", "bb", "ccc"))
        } yield {
          assertTrue(result == List("a", "bb", "ccc"))
        }
      }
      test("accumulate violations with indices") {
        given Validation[Violation, String, String] = Validations.minLength(3)
        val v                                       = summon[Validation[Violation, List[String], List[String]]]

        val expectedViolations = Violations[Violation](
          children = Map(
            Path(0) -> Violations(Vector(Violation.TooShortString("ab", 3))),
            Path(2) -> Violations(Vector(Violation.TooShortString("x", 3))),
          ),
        )

        assertTrue(v.run(List("ab", "hello", "x")).is(_.left) == expectedViolations)
      }
    }
    suiteAll("mapCanBeValidatedAs") {
      test("validate all values in Map") {
        given Validation[Violation, String, String] = Validations.minLength(1)
        val v                                       = summon[Validation[Violation, Map[String, String], Map[String, String]]]
        val input                                   = Map(
          "a" -> "x",
          "b" -> "yy",
        )

        for {
          result <- v.run(input)
        } yield {
          assertTrue(result == input)
        }
      }
      test("accumulate violations with keys") {
        given Validation[Violation, String, String] = Validations.minLength(3)
        val v                                       = summon[Validation[Violation, Map[String, String], Map[String, String]]]

        val expectedViolations = Violations[Violation](
          children = Map(
            Path("a") -> Violations(Vector(Violation.TooShortString("ab", 3))),
            Path("c") -> Violations(Vector(Violation.TooShortString("x", 3))),
          ),
        )

        assertTrue(v.run(Map("a" -> "ab", "b" -> "hello", "c" -> "x")).is(_.left) == expectedViolations)
      }
    }
  }
}
