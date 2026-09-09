package yoshi

import cats.data.NonEmptyList
import yoshi.defaults.*
import yoshi.interop.cats.*
import zio.test.*

object CatsNonEmptyListSpec extends ZIOSpecDefault {

  override def spec = suiteAll("NonEmptyList") {
    suiteAll("collection → NonEmptyList") {
      test("transforms every element") {
        for {
          fromList   <- List("1", "2", "3").validateAs[NonEmptyList[Int]]
          fromVector <- Vector("1", "2", "3").validateAs[NonEmptyList[Int]]
          fromSeq    <- Seq("1", "2", "3").validateAs[NonEmptyList[Int]]
        } yield {
          assertTrue(
            fromList == NonEmptyList.of(1, 2, 3),
            fromVector == NonEmptyList.of(1, 2, 3),
            fromSeq == NonEmptyList.of(1, 2, 3),
          )
        }
      }
      test("fails with the Required violation when the collection is empty") {
        assertTrue(
          List.empty[String].validateAs[NonEmptyList[Int]].is(_.left) == Violations.of(Violation.Required),
        )
      }
      test("reports the violation under the index of the invalid element") {
        assertTrue(
          List("1", "abc").validateAs[NonEmptyList[Int]].is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")).asChild(1),
        )
      }
      test("accumulates the violations of every invalid element") {
        assertTrue(
          List("abc", "2", "def").validateAs[NonEmptyList[Int]].is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")).asChild(0) ++
            Violations.of(Violation.NonIntegerString("def")).asChild(2),
        )
      }
      test("applies the element validation in scope even though the element type is unchanged") {
        given Validation[Violation, Int, Int] = Validations.positive

        assertTrue(
          List(1, -2, 3).validateAs[NonEmptyList[Int]].is(_.left) ==
            Violations.of(Violation.NonPositive(-2)).asChild(1),
        )
      }
    }
    suiteAll("NonEmptyList → NonEmptyList") {
      test("transforms every element") {
        for {
          result <- NonEmptyList.of("1", "2", "3").validateAs[NonEmptyList[Int]]
        } yield {
          assertTrue(result == NonEmptyList.of(1, 2, 3))
        }
      }
      test("reports the violation under the index of the invalid element") {
        assertTrue(
          NonEmptyList.of("1", "abc").validateAs[NonEmptyList[Int]].is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")).asChild(1),
        )
      }
      test("accumulates the violations of every invalid element") {
        assertTrue(
          NonEmptyList.of("abc", "def").validateAs[NonEmptyList[Int]].is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")).asChild(0) ++
            Violations.of(Violation.NonIntegerString("def")).asChild(1),
        )
      }
    }
  }
}
